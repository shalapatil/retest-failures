(ns leiningen.hooks.retest-failures
  (:use [robert.hooke :only [add-hook]]
        [leiningen.test :only [form-for-testing-namespaces
                               *exit-after-tests*]])
  (:require [retest-failures.constants :refer [temp-failure-file]]
            [clojure.java.io :as cio])
  (:import [java.io File]))

(defn add-failures-to-temp-file
  [form-for-testing & args]
  (let [orig-exit-after-tests *exit-after-tests*]
    (binding [*exit-after-tests* false]
      `(let [failure-file# ".lein-failures-temp"
             failures# (if (.exists (File. failure-file#))
                         (->> (slurp failure-file#) read-string)
                         {})]
         (try (require '~'robert.hooke)
              ((resolve 'robert.hooke/add-hook)
               (resolve 'clojure.test/report)
               (fn report-with-failures [report# m# & args#]
                 (when (#{:error :fail} (:type m#))
                   (when-let [first-var# (-> clojure.test/*testing-vars* first meta)]
                     (let [ns-name# (-> first-var# :ns ns-name name)
                           test-name# (-> first-var# :name name)]
                       (with-open [w# (clojure.java.io/writer failure-file#)]
                         (.write w# (str (update-in failures# [ns-name#] conj test-name#)))))))
                 (apply report# m# args#)))
              (catch Exception _#
                (println "retest requires robert/hooke dep.")))
         (with-redefs [spit (fn [_# _#])]
           ~(apply form-for-testing args))
         (when ~orig-exit-after-tests
           (System/exit 0))))))

(defn activate []
  (add-hook #'form-for-testing-namespaces
            #'add-failures-to-temp-file))
