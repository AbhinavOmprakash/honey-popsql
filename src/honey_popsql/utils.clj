(ns honey-popsql.utils)

(defn extract-table-name
  "Expects a honeysql map and extracts the table name."
  [{:keys [from insert-into update delete-from]}]
  (->> [from insert-into update delete-from]
       (filter some?)
       flatten
       first))


(defn agg-queries-for-table
  "Takes in honeysql map(s) and returns a map of table-name 
  and a list of all queries on that table."
  [acc {:keys [table-name] :as m}]
  (update acc table-name #(conj % m)))


(defn agg-by-tag
  [[k v]]
  (let [new-v (reduce (fn [a {:keys [tag] :as m}]
                        (update a tag #(if (nil? %)
                                         (conj  [] (:select-query m))
                                         (conj  % (:select-query m)))))
                      {}
                      v)]
    [k new-v]))


(defn normalize
  [m]
  (if (:query m)
    m
    {:query m
     :tag nil}))
