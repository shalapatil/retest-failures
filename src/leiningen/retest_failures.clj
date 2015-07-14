(ns leiningen.retest-failures
  (:require [leiningen.test :as test]
            [leiningen.hooks.retest-failures :as hooks]))

(def failure-file ".lein-failures")
(def temp-failure-file ".lein-failures-temp")

(defn- print-line
  []
  (println "---------------------------------------------------------------------"))

(defn- get-failed-count
  [tests]
  (-> tests
      vals
      flatten
      count))

(defn- read-file
  [file-name]
  (->> (slurp file-name)
       read-string))

(defn- initialize-temp-file
  []
  (spit temp-failure-file {}))

(defn- execute-tests
  [project failed-tests]
  (doseq [[ns tests] failed-tests]
    (doseq [test-name tests]
      (test/test project ":only" (str ns "/" test-name))
      (print-line))))

(defn- update-failure-file-with-latest-results
  [failed-tests]
  (spit failure-file failed-tests))

(defn retest-failures
  "Tests only failed tests from last round of test."
  [project & args]
  (initialize-temp-file)
  (hooks/activate)
  (let [failed-tests (read-file failure-file)
        last-run-failed-count (get-failed-count failed-tests)]
    (println "\nTotal failures/errors from last round: " last-run-failed-count "\n")
    (print-line)
    (execute-tests project failed-tests)
    (let [failed-tests* (read-file temp-failure-file)]
      (println (str "\n" (get-failed-count failed-tests*) "/" last-run-failed-count
                    " tests resulted in failure/error. \n"))
      (update-failure-file-with-latest-results failed-tests*))))
