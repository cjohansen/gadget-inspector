package main

import (
	"encoding/json"
	"fmt"
	"io/ioutil"
	"net/http"
	"os"
	"strings"
)

type AppRequest struct {
	UserAgent string `json:"userAgent"`
	Host      string `json:"host"`
}

type App struct {
	Id          string
	UserAgent   string
	Host        string
	Messages    chan *Message
	Connections map[chan *Message]bool
	LastPayload *Message
}

type Message struct {
	Event string
	Data  string
}

type Hub struct {
	Apps      map[string]*App
	CurrentId int
}

func Cors(headers *http.Header) {
	headers.Set("Access-Control-Allow-Origin", "*")
	headers.Set("Access-Control-Allow-Methods", "GET, POST, OPTIONS")
	headers.Set("Access-Control-Allow-Headers", "Content-Type")
}

func GetAppId(r *http.Request) string {
	return strings.Split(r.URL.Path, "/")[2]
}

func ConnectClient(app *App) chan *Message {
	connection := make(chan *Message)

	if app.LastPayload != nil {
		go func() { connection <- app.LastPayload }()
	}

	return connection
}

func EventStream(app *App, w http.ResponseWriter, r *http.Request) {
	f, ok := w.(http.Flusher)

	if !ok {
		http.Error(w, "Streaming unsupported!", http.StatusInternalServerError)
		return
	}

	c, ok := w.(http.CloseNotifier)
	if !ok {
		http.Error(w, "Close notification unsupported", http.StatusInternalServerError)
		return
	}

	connection := ConnectClient(app)
	app.Connections[connection] = true

	defer func() {
		delete(app.Connections, connection)
	}()

	headers := w.Header()
	Cors(&headers)
	headers.Set("Content-Type", "text/event-stream")
	headers.Set("Cache-Control", "no-cache")
	headers.Set("Connection", "keep-alive")
	f.Flush()
	closer := c.CloseNotify()

	for {
		select {
		case msg := <-connection:
			fmt.Fprintf(w, "event: %s\n", msg.Event)
			fmt.Fprintf(w, "data: %s\n\n", msg.Data)
			f.Flush()
		case <-closer:
			return
		}
	}
}

func ReceiveEvent(app *App, event string, w http.ResponseWriter, r *http.Request) {
	defer r.Body.Close()
	body, _ := ioutil.ReadAll(r.Body)

	go func() {
		app.Messages <- &Message{event, string(body)}
	}()

	headers := w.Header()
	Cors(&headers)
	headers.Set("Content-Type", "application/json")
}

func ServeEvents(hub *Hub) func(http.ResponseWriter, *http.Request) {
	return func(w http.ResponseWriter, r *http.Request) {
		appId := GetAppId(r)
		switch r.Method {
		case http.MethodGet:
			EventStream(hub.Apps[appId], w, r)
		case http.MethodPost:
			ReceiveEvent(hub.Apps[appId], "event", w, r)
		case http.MethodOptions:
			headers := w.Header()
			Cors(&headers)
		default:
			http.Error(w, "Invalid request method.", 405)
		}
	}
}

func ClientResponse(w http.ResponseWriter, id string) {
	headers := w.Header()
	Cors(&headers)
	headers.Set("Content-Type", "application/json")
	fmt.Fprintf(w, "{\"id\":\"%s\"}\n", id)
}

func RegisterClient(hub *Hub, w http.ResponseWriter, r *http.Request) {
	defer r.Body.Close()
	req := AppRequest{}
	body, _ := ioutil.ReadAll(r.Body)
	json.Unmarshal(body, &req)

	for id, app := range hub.Apps {
		if app.UserAgent == req.UserAgent && app.Host == req.Host {
			ClientResponse(w, id)
			return
		}
	}

	id := fmt.Sprintf("%d", hub.CurrentId)
	fmt.Printf("Register client %s %s => %s\n", req.Host, req.UserAgent, id)
	hub.CurrentId += 1
	app := &App{
		id,
		req.UserAgent,
		req.Host,
		make(chan *Message),
		make(map[chan *Message]bool),
		nil,
	}

	hub.Apps[id] = app

	go func() {
		for {
			select {
			case msg := <-app.Messages:
				for s, _ := range app.Connections {
					s <- msg
				}
				if msg.Event == "event" {
					app.LastPayload = msg
				}
			}
		}
	}()

	ClientResponse(w, id)
}

func ClientRegistrator(hub *Hub) func(http.ResponseWriter, *http.Request) {
	return func(w http.ResponseWriter, r *http.Request) {
		switch r.Method {
		case http.MethodPost:
			RegisterClient(hub, w, r)
		case http.MethodOptions:
			headers := w.Header()
			Cors(&headers)
		default:
			http.Error(w, "Invalid request method.", 405)
		}
	}
}

func port() string {
	if len(os.Args) > 1 {
		return os.Args[1]
	} else {
		return "7117"
	}
}

func Inspector(w http.ResponseWriter, r *http.Request) {
	headers := w.Header()
	Cors(&headers)
	headers.Set("Content-Type", "text/html")
	headers.Set("Cache-Control", "no-cache")

	fmt.Fprintln(w, `<!DOCTYPE html>
<html>
	<head>
		<title>ClojureScipt Data Browser - Remote Inspector</title>
		<link rel="stylesheet" type="text/css" href="/assets/inspector.css">
	</head>
	<body>
		<div id="app"></div>
		<script src="/assets/remote-inspector.js"></script>
	</body>
</html>
`)
}

func Index(hub *Hub) func(http.ResponseWriter, *http.Request) {
	return func(w http.ResponseWriter, r *http.Request) {
		headers := w.Header()
		Cors(&headers)
		headers.Set("Content-Type", "text/html")
		headers.Set("Cache-Control", "no-cache")
		links := ""

		for id, app := range hub.Apps {
			links += fmt.Sprintf("<p><a href=\"/inspect/%s\">%s %s</p>", id, app.Host, app.UserAgent)
		}

		fmt.Fprintf(w, `<!DOCTYPE html>
<html>
	<head>
		<title>ClojureScript Data Browser - Remote Inspector</title>
	</head>
	<body>
		%s
	</body>
</html>`, links)
	}
}

func main() {
	hub := &Hub{make(map[string]*App), 0}
	http.HandleFunc("/clients", ClientRegistrator(hub))
	http.HandleFunc("/events/", ServeEvents(hub))
	http.HandleFunc("/actions/", func(w http.ResponseWriter, r *http.Request) {
		ReceiveEvent(hub.Apps[GetAppId(r)], "action", w, r)
	})
	http.HandleFunc("/", Index(hub))
	http.HandleFunc("/inspect/", Inspector)
	http.Handle("/assets/", http.FileServer(http.Dir("./static")))
	fmt.Println("http://localhost:" + port())
	http.ListenAndServe(":"+port(), nil)
}
