(ns ^:figwheel-hooks gadget.dev
  (:require [cljs-data-browser.actions :as actions]
            [gadget.core :as g]
            [gadget.dev-renderer]
            [gadget.extensions]))

(def data
  {:apiKey "POK3uZiVx+aiWcwHKQXZ9N0ENcV9aWnoEmsLMnQhYezssdfhj78"
   :properties [{:propertyId "1b8c12ae-7896-3fcc-bfde-dbe9505d04ec"
                 :name "Simpson's street"
                 :profile {:address {:streetAddress nil
                                     :postalCode "0987"
                                     :city "Oslo"
                                     :countryCode "NO"}
                           :updatedAt "2018-09-12T10:15:26.232Z"
                           :createdAt "2015-12-16T14:45:02.071Z"
                           :meterPointId nil
                           :profileCompleteness 100
                           :pid "1b8c12ae-9087-3fcc-bfde-dbe9505d04ec"
                           :id 162
                           :userId "8cdca74b-6754-49c5-8c72-2d237061c366"
                           :customerNumber nil
                           :profile {:area 62
                                     :propertyType "apartment"
                                     :ownership "selfOwned"
                                     :numElectricVehicles 2
                                     :hotWaterProvided true
                                     :builtYear 1938
                                     :heatingSource ["districtHeating" "heatingCables"]
                                     :numOccupants 1}
                           :contactId nil}}
                {:propertyId "f91b60b9-9089-4386-9118-089ace6aa57e"
                 :name "Balony Street 45"
                 :profile {:address {:streetAddress "Balony Street 45"
                                     :postalCode "9087"
                                     :city "Oppgård"
                                     :countryCode "NO"}
                           :updatedAt "2018-09-06T07:00:50.897Z"
                           :createdAt "2016-12-25T12:07:53.339Z"
                           :meterPointId nil
                           :profileCompleteness 87
                           :pid "f91b60b9-b0d3-9087-9118-089ace6aa57e"
                           :id 160716
                           :userId "8cdca74b-acbf-49c5-8c72-2d237061c366"
                           :customerNumber nil
                           :profile {:area 36
                                     :propertyType "apartment"
                                     :ownership "selfOwned"
                                     :numElectricVehicles nil
                                     :hotWaterProvided true
                                     :builtYear 1982
                                     :heatingSource ["electricRadiator"]
                                     :numOccupants 1}
                           :contactId nil}}]

   :invoices (list {:dueamount 182.31, :kid "755120058905", :due-date "2018-11-27T00:00:00", :invoice-no "518009794", :code "", :account-no "", :name ""}
                   {:dueamount 571.71, :kid "677257790289", :due-date "2018-10-22T00:00:00", :invoice-no "517495697", :code "BET", :account-no "", :name "Betalt/Nullfaktura"}
                   {:dueamount 657.28, :kid "854691834214", :due-date "2018-09-21T00:00:00", :invoice-no "517188187", :code "BET", :account-no "", :name "Betalt/Nullfaktura"}
                   {:dueamount 167.00, :kid "883715075780", :due-date "2018-08-22T00:00:00", :invoice-no "516911850", :code "BET", :account-no "", :name "Betalt/Nullfaktura"}
                   {:dueamount 880.76, :kid "453797521051", :due-date "2018-07-20T00:00:00", :invoice-no "516610066", :code "BET", :account-no "", :name "Betalt/Nullfaktura"}
                   {:dueamount 241.31, :kid "140947663229", :due-date "2018-06-22T00:00:00", :invoice-no "516409482", :code "BET", :account-no "", :name "Betalt/Nullfaktura"}
                   {:dueamount 630.28, :kid "356529002769", :due-date "2018-05-23T00:00:00", :invoice-no "516225135", :code "BET", :account-no "", :name "Betalt/Nullfaktura"}
                   {:dueamount 703.90, :kid "545410389517", :due-date "2018-04-24T00:00:00", :invoice-no "515840338", :code "BET", :account-no "", :name "Betalt/Nullfaktura"}
                   {:dueamount 640.38, :kid "453365045095", :due-date "2018-03-22T00:00:00", :invoice-no "515519994", :code "BET", :account-no "", :name "Betalt/Nullfaktura"}
                   {:dueamount 577.17, :kid "270651069889", :due-date "2018-02-22T00:00:00", :invoice-no "515176103", :code "BET", :account-no "", :name "Betalt/Nullfaktura"}
                   {:dueamount 610.36, :kid "971035455271", :due-date "2018-01-24T00:00:00", :invoice-no "514960204", :code "BET", :account-no "", :name "Betalt/Nullfaktura"}
                   {:dueamount 696.55, :kid "596937912072", :due-date "2017-12-27T00:00:00", :invoice-no "514719153", :code "BET", :account-no "", :name "Betalt/Nullfaktura"}
                   {:dueamount 481.01, :kid "352693584775", :due-date "2017-11-21T00:00:00", :invoice-no "514272566", :code "BET", :account-no "", :name "Betalt/Nullfaktura"}
                   {:dueamount 371.72, :kid "148179877297", :due-date "2017-10-24T00:00:00", :invoice-no "514025990", :code "BET", :account-no "", :name "Betalt/Nullfaktura"}
                   {:dueamount 459.42, :kid "199832117441", :due-date "2017-09-25T00:00:00", :invoice-no "513735766", :code "BET", :account-no "", :name "Betalt/Nullfaktura"}
                   {:dueamount 342.60, :kid "123999361635", :due-date "2017-08-25T00:00:00", :invoice-no "513340947", :code "BET", :account-no "", :name "Betalt/Nullfaktura"}
                   {:dueamount 217.91, :kid "134093984825", :due-date "2017-07-21T00:00:00", :invoice-no "513079999", :code "BET", :account-no "", :name "Betalt/Nullfaktura"}
                   {:dueamount 684.89, :kid "137877613122", :due-date "2017-07-06T00:00:00", :invoice-no "512906993", :code "BET", :account-no "", :name "Betalt/Nullfaktura"}),

   :server-time-diff 294,
   :power-meters nil,
   :settings {:surveys {:insight-control-complete true},
              :analytics? true},
   :history (list {:page-id :insight-page}
                  {:page-id :home-page}),
   :link-app.director/reloads 1,
   :size {:w 960, :h 479},
   :token "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiIxMjM0NTY3ODkwIiwibmFtZSI6IkpvaG4gRG9lIiwiaWF0IjoxNTE2MjM5MDIyLCJlbWFpbCI6ImNocmlzdGlhbkBjam9oYW5zZW4ubm8ifQ.Yk2B6VSt13rq8NoLabeJPhLsSnX7H3T_8rMw9O_OFlU",
   :tokens ["eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiIxMjM0NTY3ODkwIiwibmFtZSI6IkNocmlzIERvZSIsImlhdCI6MTUxNjIzOTAyMiwiZW1haWwiOiJjaHJpc3RpYW5AY2pvaGFuc2VuLm5vIn0.1u6jhPhcEmUPmA9Qu1m-Lzt-sHRtsw3QqVQAqcuMCkM"
            "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiIxMjM0NTY3ODkwIiwibmFtZSI6IkNocmlzIERvZSIsImlhdCI6MTUxNjIzOTAyMiwiZW1haWwiOiJjaHJpc3RpYW4rc2Vjb25kQGNqb2hhbnNlbi5ubyJ9.SXC3bJJl6pq2XxF5vzSunqwLDhJ_sjLqgTodNMCtAdw"
            "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiIxMjM0NTY3ODkwIiwibmFtZSI6IkNocmlzIERvZSIsImlhdCI6MTUxNjIzOTAyMiwiZW1haWwiOiJjaHJpc3RpYW4rdGhpcmRAY2pvaGFuc2VuLm5vIn0.udvCGioKHoVDRAVJKzBcQApNMGSEbtIY_Ukh05lsUGI"]
   :status {:remember {:token-ok true, :connectivity-up true}, :last-server-ping 1543010461879, :connectivity :up},
   :disabled-flags [],
   :current-facility {:meter-number "7359992899999999",
                      :is-net-customer true,
                      :city "Oslo",
                      :zipcode "0987",
                      :consumption [{:timestamp "2016-01-01", :value 0}
                                    {:timestamp "2016-02-01", :value 0}
                                    {:timestamp "2016-03-01", :value 0}
                                    {:timestamp "2016-04-01", :value 0}
                                    {:timestamp "2016-05-01", :value 0}
                                    {:timestamp "2016-06-01", :value 322}
                                    {:timestamp "2016-07-01", :value 306}
                                    {:timestamp "2016-08-01", :value 405}
                                    {:timestamp "2016-09-01", :value 316}
                                    {:timestamp "2016-10-01", :value 323}
                                    {:timestamp "2016-11-01", :value 370}
                                    {:timestamp "2016-12-01", :value 355}
                                    {:timestamp "2017-01-01", :value 355}
                                    {:timestamp "2017-02-01", :value 325}
                                    {:timestamp "2017-03-01", :value 344}
                                    {:timestamp "2017-04-01", :value 310}
                                    {:timestamp "2017-05-01", :value 393}
                                    {:timestamp "2017-06-01", :value 389}
                                    {:timestamp "2017-07-01", :value 400}
                                    {:timestamp "2017-08-01", :value 302}
                                    {:timestamp "2017-09-01", :value 419}
                                    {:timestamp "2017-10-01", :value 355}
                                    {:timestamp "2017-11-01", :value 354}
                                    {:timestamp "2017-12-01", :value 352}
                                    {:timestamp "2018-01-01", :value 393}
                                    {:timestamp "2018-02-01", :value 350}
                                    {:timestamp "2018-03-01", :value 329}
                                    {:timestamp "2018-04-01", :value 339}
                                    {:timestamp "2018-05-01", :value 348}
                                    {:timestamp "2018-06-01", :value 351}
                                    {:timestamp "2018-07-01", :value 322}
                                    {:timestamp "2018-08-01", :value 279}
                                    {:timestamp "2018-09-01", :value 320}
                                    {:timestamp "2018-10-01", :value 347}
                                    {:timestamp "2018-11-01", :value 255}],
                      :meter-reading-type "T",
                      :street-address "Simpson's street",
                      :meter-point-id "707057500052666666",
                      :last-reading {:from-date "2018-04-16",
                                     :reading-value 4781,
                                     :registered-by-customer false}},
   :gopher {[:current-facility-estimated-consumption] {:requested 1543010387464,
                                                       :loaded 1543010389173},
            [:current-facility-estimated-consumption-daily] {:requested 1543010387536,
                                                             :loaded 1543010389254},
            [:settings] {:requested 1543010387611, :loaded 1543010388018}},
   :app {:env-source :config, :version "3.2.0"},
   :session-info {:id "d3dfa5e9-1c33-1542-a659-7d975e57222f", :set-at 1543010379807, :git-sha "96bede7"},
   :login-recovery {:available? false},
   :current-facility-estimated-consumption-daily {:energy-breakdown {:appliances 0.35, :lighting 0.41, :electric-vehicles 0, :heating 0.18, :hot-water 0, :electronics 0.06}, :end-date "2018-11-24T00:00:00.000Z", :consumption [16 16 16 16 16 16 16 16 18 18 18 18 17 17 17 17 17 17 17 17 20 20 17], :interval "daily", :start-date "2018-11-01T00:00:00.000Z", :from "2018-11", :to "2018-11-24"}})

(def store (g/create-atom "App state" data))

(comment
  (swap! store assoc :now (js/Date.))
  (swap! store with-meta {:lol 42})

  (meta @store)
)

(g/inspect "Page data"{:title "Some page"
                       :description "Some description"})

(defmethod actions/exec-action :default [payload]
  (g/action payload))

(defn ^:after-load render-on-relaod []
  (g/render-inspector))

(render-on-relaod)

(actions/exec-action
 (str "{:action :set-window-size :args [{:width " js/window.innerWidth
      " :height " js/window.innerHeight "}]}"))
