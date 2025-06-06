^{:clay {:quarto {:draft true}}}
(ns civitas.explorer.db
  (:require [clojure.edn :as edn]
            [clojure.pprint :as pprint]
            [clojure.walk :as walk]
            [tablecloth.api :as tc]
            [clj-yaml.core :as yaml]))

(walk/postwalk
  (fn [x]
    (cond (map? x) (into {} x)
          (seq? x) (into [] x)
          :else x))
  (yaml/parse-string (slurp "site/_quarto.yml")))

;; TODO: it might be more convenient to use Quarto to gather the metadata
;; ```
;; quarto list --to json
;; ```

(def db-file "site/db.edn")

(defn spit-edn [f content]
  (spit f (with-out-str (pprint/pprint content))))

(defn slurp-edn [f]
  (edn/read-string (slurp f)))

(def db (atom (slurp-edn db-file)))

;; TODO: what if the front matter doesn't match existing?

(defn set-notebooks [notebooks]
  (->> {:notebooks notebooks}
       (reset! db)
       (spit-edn db-file)))

(defn index-by
  "Return a map where a key is (f item) and a value is item."
  [f coll]
  (persistent!
    (reduce
      (fn [ret x]
        (assoc! ret (f x) x))
      (transient {}) coll)))

(defn notebooks-ds []
  (tc/dataset (:notebooks @db)))

(defn notebooks []
  (:notebooks @db))

(defn topics-ds []
  (tc/dataset (:topics @db)))

(defn topics []
  (:topics @db))

;; TODO: this is a terrible way to do it
(def get-topic
  (index-by :id (:topics @db)))

(defn get-notebooks-by-topic []
  (-> (group-by (comp first :topics) (:notebooks @db))
      (update-vals #(map-indexed (fn [idx x]
                                   (assoc x :position idx))
                                 %))))

(def get-colors
  (vec (keep :color (:topics @db))))
