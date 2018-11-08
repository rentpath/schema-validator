(ns registry-sync.core
  (:gen-class)
  (:require [abracad.avro :as avro]
            [clojure.tools.cli :refer [parse-opts]]
            [cheshire.core :as json]
            [clj-http.client :as http]
            [clojure.string :as str]
            [clojure.java.io :as io])
  (:import [com.hotels.avro.compatibility Compatibility Compatibility$Mode]))

(def ^:dynamic failed false)

(defn set-failed! []
  (alter-var-root #'failed (constantly true)))

(def cli-options
  [["-r" "--registry-url URL" "Avro schema registry url"
    :required "URL"
    :id :registry-url]
   ["-d" "--schema-dir PATH" "Path to directory of schemas to update"
    :required "PATH"
    :id :schema-dir]
   ["-h" "--help"]])

(def compatibility-modes
  {"none" true
   "backwards" Compatibility$Mode/CAN_READ_LATEST
   "backwards-transitive" Compatibility$Mode/CAN_READ_ALL
   "forwards" Compatibility$Mode/CAN_BE_READ_BY_LATEST
   "forwards-transitive" Compatibility$Mode/CAN_BE_READ_BY_ALL
   "full" Compatibility$Mode/MUTUAL_READ_WITH_LATEST
   "full-transitive" Compatibility$Mode/MUTUAL_READ_WITH_LATEST})

(def valid-compatibility-modes (set (keys compatibility-modes)))

(defn get-compatibility
  [schema-path]
  (let [raw-schema (slurp schema-path)
        {:keys [name compatibility]} (json/parse-string raw-schema true)]
    (if (valid-compatibility-modes compatibility)
      compatibility
      (throw (ex-info (str "Invalid compatibility mode '" compatibility "' for proposed schema at " schema-path ".\n"
                           "Must be one of " (pr-str (into [] valid-compatibility-modes))) {})))))

(defn update-schema
  [registry-url subject schema]
  (http/post (format "%s/subjects/%s/versions" registry-url subject)
             {:body schema
              :content-type :json
              :as :json}))

(defn get-versions
  [registry-url subject]
  (:body (http/get (format "%s/subjects/%s/versions" registry-url subject)
                   {:as :json})))

(defn get-schema-by-subject-versions
  [registry-url subject version]
  (:body (http/get (format "%s/subjects/%s/versions/%s" registry-url subject version)
                   {:as :json})))

(defn get-all-schemas-for-subject
  [registry-url subject]
  (let [versions (try
                   (get-versions registry-url subject)
                   (catch Exception e
                     (cond (= 404 (:status (ex-data e)))
                           (do (println (format "No previous versions found for subject %s on %s. Skipping." subject registry-url))
                               [])
                           :else (throw e))))]
    (mapv #(get-schema-by-subject-versions registry-url subject %) versions)))

(defn check-compatibility
  [mode proposed-schema registered-schemas]
  (.check (compatibility-modes mode)
          proposed-schema
          registered-schemas))

(defn format-results
  [compatibility-results]
  (mapv (fn [result]
          {:reader (str (.getReader result))
           :writer (str (.getWriter result))
           :message (.asMessage result)})
        compatibility-results))

(defn latest-schema
  [registered-schemas]
  (:parsed-schema (last registered-schemas)))

(defn parse-schema-response
  [response]
  (assoc response :parsed-schema (avro/parse-schema (:schema response))))

(defn validate-proposed-schema
  [registry-url compatibility-mode proposed-schema subject]
  (let [latest-schema (->> (get-all-schemas-for-subject registry-url subject)
                           (mapv parse-schema-response)
                           (latest-schema))]
    (reduce (fn [valid? result]
              (println (.asMessage result))
              (and valid? (.isCompatible result)))
            true
            (if (= "none" compatibility-mode)
              (println (format "Desired compatibility for subject %s on is set to 'none'. Skipping." subject))
              (.getResults (check-compatibility compatibility-mode proposed-schema [latest-schema]))))))

(defn parse-proposed-schema
  [schema-path]
  (avro/parse-schema (slurp schema-path)))

(defn get-filename
  [path]
  (-> path
      (str/split #"/")
      (last)
      (str/split #"\.")
      (first)))

(defn check-schema-compatibility
  [registry-url schema-path]
  (let [subject (get-filename schema-path)
        _ (println)
        _ (println (format "Comparing proposed schema %s with latest schema on subject %s" schema-path subject))
        proposed-schema (try (parse-proposed-schema schema-path)
                             (catch Exception e
                               (set-failed!)
                               (println (format "Failed to parse proposed avro schema %s. Is it valid avro?" schema-path))))
        compatibility-mode (try (get-compatibility schema-path)
                                (catch Exception e
                                  (set-failed!)
                                  (println (.getMessage e))))]
    (when (and proposed-schema compatibility-mode)
      (when-not (validate-proposed-schema registry-url compatibility-mode proposed-schema subject)
        (set-failed!)))))

(defn get-proposed-schemas
  [schema-dir-path]
  (->> schema-dir-path
       (io/file)
       (file-seq)
       (filter #(.isFile %))
       (map #(.getPath %))))

(defn validate-options
  [options]
  (when-not (:help options)
    (assert (:registry-url options) "must provide a --registry-url")
    (assert (:schema-dir options) "must provide a --schema-dir")))

(defn usage
  [options-summary]
  (->> ["schema-validator - validates proposed avro schemas against a schema registry"
        ""
        "Usage: schema-validator [options]"
        ""
        "Options:"
        options-summary
        ""]
       (str/join \newline)))

(defn -main
  [& args]
  (let [{:keys [options arguments summary]} (parse-opts args cli-options)]
    (validate-options options)
    (cond
      (:help options)
      (println (usage summary))

      :else
      (do
        (mapv #(check-schema-compatibility (:registry-url options) %)
              (get-proposed-schemas (:schema-dir options)))
        (println)
        (if failed
          (do
            (println "[Error] Invalid schemas found.")
            (System/exit 1))

          (do
            (println "[Success] All newly proposed schemas are compatible.")
            (System/exit 0)))))))
