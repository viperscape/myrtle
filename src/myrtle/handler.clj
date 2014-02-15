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
      [clojure.tools.nrepl.middleware :as nreplmiddle]
      [clojure.java.io :as io]
      [me.raynes.fs :as fs]))



(def buffers (ref {}))

(defn repl-buffer [b]
   (load-string b))

(defn nrepl-buffer [e b]
     (-> (:nrepl b)
       (nrepl/message {:op :eval :code e :session (:nrepl-id b)})
       doall))

(defn get-buffer [bid]
  (let [b (bid @buffers)]
    ;(send! ch (json/write-str b))
  ))

(defn new-buffer [ch]
  (let [bid (keyword (gensym "buff"))
        ncli (nrepl/client (nrepl/connect :port 7888) 1000)
        nid (nrepl/new-session ncli)]
   (dosync (alter buffers conj {bid {:ch ch, :nrepl ncli :nrepl-id nid}}))
   bid))


(defn async-handler [request]
 (println "connection from " (:remote-addr request) "; " (.startsWith (:remote-addr request) "192.168.2"))
 (if (.startsWith (:remote-addr request) "192.168.2")
  (with-channel request channel
    (on-close channel (fn [status] (println channel " channel closed: " status)))
    (if (websocket? channel)
      
    	(on-receive channel (fn [data] 
        (let [req (json/read-str data :key-fn keyword)]
          (prn channel " in-channel data: " req)

          (if-let [buff (:buffer req)] ;;note 'open file as buffer' expects file to exist, not really put thru paces either
            (do ;; these should be in a switch case block
              (if (:open buff) (if-not (clojure.string/blank? (:open buff))
                (if-let [b ((keyword (:open buff)) @buffers)]
                  (send! channel (json/write-str {:console (str "opening buffer " b)})))
                 ; (send! channel (json/write-str {:console (str "opening file as buffer " buff),
                 ;                                 :file {:contents (slurp buff),:extension (fs/extension d),:filename d}})))
                (send! channel (json/write-str {:buffer (new-buffer channel)}))))
              (if (:save buff) (send! channel (json/write-str {:console "saving buffer"})))
              (if (:kill buff) (send! channel (json/write-str {:console "killing buffer"})))
              (if (:list buff) (send! channel (json/write-str {:console (str "buffer list: " (keys @buffers))})))
              ))


           (if-let [f (:file req)]
            (if-let [d (:list f)]
              (let [d (or (if-not (clojure.string/blank? d) d)
                           (System/getProperty "user.dir"))] ;; if blank use home dir.
              (send! channel (json/write-str {:file 
                                              (if-not (fs/file? d) ;; then list dir
                                                {:list (conj (map #(apply str d "/" %) 
                                                                  (fs/list-dir d)) (str(fs/parent d)))}
                                                (let [fc {:contents (slurp d),
                                                          :extension (fs/extension d),
                                                          :filename d}]
                                                  (dosync (alter buffers update-in [(keyword(:buffer f)) :file] (constantly fc)))
                                                  fc))}))
              )))

          (if-let [repl-req (:repl req)] (send! channel  (json/write-str {:repl (vec (or  
            (map json/write-str(filter :value 
              (nrepl-buffer (:eval repl-req) ((keyword(:buffer repl-req))@buffers))));(:nrepl((keyword(:buffer repl-req))@buffers)) )));nch)));
            "nil"))})))

          (if (:completions req) (send! channel  (json/write-str {:completions (or  
            (try (let [c (vec (completions (:completions req)))] (if-not (empty? c) c "nil"))
              (catch Exception e (.getMessage e))) "nil")})))
          )))
      (send! channel (slurp "resources/buffer.html"))))
  ))

(defroutes allroutes
  (GET "/" [] async-handler)
  ;;ignore below routes! 
  ;(GET "/buffers" [] (resp/file-response "resources/buffer.html"));{ch :async-channel} (str ch))
  ;(GET "/buffer/:bid" {ch :async-handler {bid :bid} :params} (get-buffer ch bid)) ;;{ch :async-channel, bid :bid}
  ;(GET "/buffer" {ch :async-channel} (resp/redirect (str "/buffer/" (name (new-buffer ch)))))
  (route/files "/" {:root "resources/public"})
  (route/resources "/")
  (route/not-found "404"))


(defonce server (run-server (site #'allroutes) {:port 8080}))
(defonce replserver (nreplserver/start-server :port 7888))

(defn -main
  [& args]
  )
