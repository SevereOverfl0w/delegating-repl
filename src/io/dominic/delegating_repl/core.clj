(ns io.dominic.delegating-repl.core
  (:require
    [cljs.repl :as repl]
    [clojure.java.shell :as sh]))

(defrecord DelegatingEnv [delegate]
  repl/IJavaScriptEnv
  (-setup [this opts]
    (let [result (repl/-setup delegate opts)
          !exec (future (apply sh/sh (get opts :launch-command)))]
      (try
        ;; Check command didn't fail within 1s, indicating some obvious
        ;; failure.
        (when-let [{:keys [exit out err]} (deref !exec 1000 nil)]
          (when (pos? exit)
            (println "Failed to run launch-command: "
                     (get opts :launch-command))
            (println "STDOUT:\n" (String. out))
            (println "STDERR:\n" (String. err))))
        (catch Throwable t
          (println "Failed to run launch-command: "
                   (get opts :launch-command))
          (.printStackTrace t)))
      result))
  (-evaluate [this filename line js]
    (repl/-evaluate delegate filename line js))
  (-load [this provides url]
    (repl/-load delegate provides url))
  (-tear-down [this]
    (repl/-tear-down delegate))
  repl/IReplEnvOptions
  (-repl-options [this]
    ;; TODO: Look into
    #_{:browser-repl true
     :repl-requires
     '[[clojure.browser.repl] [clojure.browser.repl.preload]]
     :cljs.cli/commands
     {:groups {::repl {:desc "browser REPL options"}}
      :init
      {["-H" "--host"]
       {:group ::repl :fn #(assoc-in %1 [:repl-env-options :host] %2)
        :arg "address"
        :doc "Address to bind"}
       ["-p" "--port"]
       {:group ::repl :fn #(assoc-in %1 [:repl-env-options :port] (Integer/parseInt %2))
        :arg "number"
        :doc "Port to bind"}}}}
    (repl/-repl-options delegate))
  repl/IParseStacktrace
  (-parse-stacktrace [this st err opts]
    (repl/-parse-stacktrace delegate st err opts))
  repl/IGetError
  (-get-error [this e env opts]
    (repl/-get-error delegate e env opts)))

(defn repl-env
  [& {:as compiler-opts}]
  (let [{repl-env-f :io.dominic.delegating-repl/repl-env}
        compiler-opts
        repl-env (apply (requiring-resolve repl-env-f)
                        (apply concat compiler-opts))]
    (merge (->DelegatingEnv repl-env)
           ;; Adds any keys that things might be poking at.
           repl-env)))
