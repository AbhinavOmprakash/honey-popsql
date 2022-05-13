(ns honey-popsql.postgres
  (:require
    [honey-popsql.core :refer [annotate]]
    [honey-popsql.utils :refer [extract-table-name
                                normalize
                                agg-by-tag]]))


(def aliases
  (for [a (seq "abcdefghijklmnopqrstuvwxyz")
        b (seq "abcdefghijklmnopqrstuvwxyz")
        c (seq "abcdefghijklmnopqrstuvwxyz")]
    ;; simplistic name mangling with hpop
    (keyword (str "hpop_" a b c))))


(defn add-returning
  [m]
  (if (not= :select (first (keys m)))
    (update m :returning #(or % [:*]))
    m))


(defn update-tag
  [{:keys [query] :as m}]
  (update m :tag (fnil identity (first (keys query)))))


(defn insert-table-name
  [{:keys [query] :as m}]
  (assoc m :table-name (extract-table-name query)))


(defn concat-jsonb-values
  "Adds a concat to all the `jsonb_agg` in the queries"
  [m]
  (into {} (map (fn [[k* v*]]
                  (if (< 1 (count v*))
                    {k* (into [:||] v*)}
                    {k* (first v*)})))
        m))


(defn query-map->jsonb-obj
  [m]
  (reduce-kv (fn [acc k v]
               ;; because k will be a keyword of the table 
               ;; we want to return a string as the name of the table
               (conj acc (name k) v))
             [:jsonb_build_object]
             m))


(defn query-map->jsonb-query
  [jsonb_obj]
  [[jsonb_obj "results"]])


(def f
  (comp
    (map normalize)
    (map insert-table-name)
    (map update-tag)))


(defn with->select-map
  [{:keys [with] :as m}]
  (update m :select (constantly (->> with
                                     (map second)
                                     (map (fn [x]
                                            {(:table-name x)
                                             [x]}))
                                     (apply
                                       merge-with into)))))


(defn add-select-query-in-with
  [[alias- query-m]]
  [alias- (-> query-m
              (update :select-query (constantly {:select [[[:jsonb_agg [:to_jsonb alias-]]]] :from alias-}))
              (update  :query add-returning))])


(defn agg-q
  [m]
  (update m :select #(into {} (map agg-by-tag %))))


(defn replace-with-query
  [m]
  (update m :with #(mapv (fn [[alias qmap]]
                           [alias (:query qmap)])
                         %)))


(defn concat-inner-jsonb
  [m]
  (update m :select
          #(into {} (map (fn [[k v]]
                           [k (concat-jsonb-values v)])
                         %))))


(defn make-inner-jsonb
  [m]
  (update m :select
          #(into {} (map (fn [[k v]]
                           [k (query-map->jsonb-obj v)])
                         %))))


(defn gen-query
  [& ms]
  ;; TODO: REFACTOR this
  (->> ms
       (into [] f)
       (interleave aliases)
       (partition-all 2)
       (map vec)
       (map add-select-query-in-with)
       (#(update {} :with (constantly (vec %))))
       with->select-map
       agg-q
       replace-with-query
       concat-inner-jsonb
       make-inner-jsonb
       (#(update % :select (comp query-map->jsonb-query
                                 query-map->jsonb-obj)))))


(comment 
  (def ms*
    [(annotate :table {:select [:a]
                       :from [:foo]})
     {:select [:b]
      :from [:foo]}
     {:select [:a]
      :from [:foo]}
     {:select [:a]
      :from [:bar]}])
  
  (apply gen-query ms*))
