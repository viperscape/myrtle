(ns myrtle.handler
  (:gen-class)
  (:use 
      [org.httpkit.server]
      [compojure.core]
      [compojure.handler]
      [complete.core])
  (:require 
      [alembic.still :as alembic]
      [compojure.route :as route]
      [ring.util.response :as resp]
      [clojure.data.json :as json]
      [clojure.tools.nrepl :as nrepl]
      [clojure.tools.nrepl.server :as nreplserver]
      [clojure.tools.nrepl.middleware :as nreplmiddle]))


(def buffers (ref {}))

(defn repl-buffer [b]
   (load-string b))

(defn nrepl-buffer [b conn]
  ;(try
   ; (with-open [conn (nrepl/connect :port 7888)]
     (-> (nrepl/client conn 1000)
       (nrepl/message {:op :eval :code b})
       doall))
    ;(catch Exception e (.getMessage e))))

(defn get-buffer [bid]
  (let [b (bid @buffers)]
    ;(send! ch (json/write-str b))
  ))

(defn new-buffer [ch]
  (let [bid (keyword (gensym "buff"))]
   (dosync (alter buffers conj {bid {:ch ch, :nrepl (nrepl/connect :port 7888)}}))
   bid))


(defn async-handler [request]
  (println "connection from " (:remote-addr request))
 (if (or (= "192.168.2.5" (:remote-addr request)) (= "127.0.0.1" (:remote-addr request)))
  (try (with-channel request channel
  	(on-close channel (fn [status] (println "channel closed: " status)))
    (if (websocket? channel)
    	(on-receive channel (fn [data] 
        (let [req (json/read-str data :key-fn keyword)]
          (prn "in-channel data: " req)

          (if-let [buff (:buffer req)]
            (do ;;it'd be nice to do this in a case block instead
              (if (:open buff) (if-not (= "" (:open buff))
                (if-let [b ((keyword (:open buff)) @buffers)]
                  (send! channel (json/write-str {:console (str "opening buffer " b)}))
                  (send! channel (json/write-str {:console (str "opening file as buffer " buff)})))
                (send! channel (json/write-str {:buffer (new-buffer channel)}))))
              (if (:save buff) (send! channel (json/write-str {:console "saving buffer"})))
              (if (:kill buff) (send! channel (json/write-str {:console "killing buffer"})))
              (if (:list buff) (send! channel (json/write-str {:console (str "buffer list: " (keys @buffers))})))
              ))

          ;(if-let [f (:file req)]
           ; (if (:list f) )

          (if-let [repl-req (:repl req)] (send! channel  (json/write-str {:repl (vec (or  
            (map json/write-str(filter :value (nrepl-buffer (:eval repl-req) (:nrepl((keyword(:buffer repl-req))@buffers)) )))
            "nil"))})))

          (if (:completions req) (send! channel  (json/write-str {:completions (or  
            (try (let [c (vec (completions (:completions req)))] (if-not (empty? c) c "nil"))
              (catch Exception e (.getMessage e))) "nil")})))
          )))
      )) ;(send! channel (slurp "resources/main.html"))))
  (catch Exception e (do 
    (println (str "caught exception: " (.getMessage e)))))
  )))

(defroutes allroutes
  (GET "/" [] async-handler)
  ;;ignore below routes! 
  (GET "/buffers" [] (resp/file-response "resources/buffer.html"));{ch :async-channel} (str ch))
  (GET "/buffer/:bid" {ch :async-handler {bid :bid} :params} (get-buffer ch bid)) ;;{ch :async-channel, bid :bid}
  (GET "/buffer" {ch :async-channel} (resp/redirect (str "/buffer/" (name (new-buffer ch)))))
  (route/files "/" {:root "resources/public"})
  (route/resources "/")
  (route/not-found "404"))


(defonce server (run-server (site #'allroutes) {:port 80}))
(defonce replserver (nreplserver/start-server :port 7888))

(defn -main
  [& args]
  )