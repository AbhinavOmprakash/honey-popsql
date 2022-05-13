(ns honey-popsql.postgres-test
  (:require
    [clojure.test :refer [deftest testing is]]
    [honey-popsql.postgres :as pg]
    [honey-popsql.test-utils :refer [jdbc-execute!
                                     conn
                                     with-tx-rollback]]))


(deftest test-select-for-two-tables
  (testing "wildcard select"
    (let [popsicles {:select [:*] :from :popsicles}
          honey {:select [:*] :from :honey}]
      (with-tx-rollback [tx conn]
        (is (= (jdbc-execute! tx (pg/gen-query popsicles honey))
               [{:results
                 {:popsicles
                  {:select
                   [{:flavor "Vanilla", :cost 10}
                    {:flavor "Chocolate", :cost 13}
                    {:flavor "Rainbow", :cost 7}
                    {:flavor "Honey", :cost 100}]},
                  :honey
                  {:select
                   [{:honey_type "Manuka honey", :cost 100}
                    {:honey_type "Sage honey", :cost 100}
                    {:honey_type "HoneySQL", :cost 1000}]}}}])))))

  (testing "Select specific columns"
    (let [popsicles {:select [:flavor] :from :popsicles}
          honey {:select [:cost] :from :honey}]
      (with-tx-rollback [tx conn]
        (is (= (jdbc-execute! tx (pg/gen-query popsicles honey))
               [{:results
                 {:popsicles
                  {:select
                   [{:flavor "Vanilla"}
                    {:flavor "Chocolate"}
                    {:flavor "Rainbow"}
                    {:flavor "Honey"}]},
                  :honey
                  {:select
                   [{:cost 100}
                    {:cost 100}
                    {:cost 1000}]}}}]))))))


(deftest test-insert-for-two-tables
  (let [popsicles {:insert-into [:popsicles]
                   :values [{:flavor "strawberry" :cost 12}]}
        honey {:insert-into [:honey]
               :values [{:honey_type "lichi" :cost 120}]}]
    (with-tx-rollback [tx conn]
      (is  (= (jdbc-execute! tx (pg/gen-query popsicles honey))
              [{:results
                {:popsicles {:insert-into [{:flavor "strawberry", :cost 12}]},
                 :honey {:insert-into [{:honey_type "lichi", :cost 120}]}}}])))))


#_(deftest temp-test

  (let [popsicles {:update :popsicles
                   :set {:flavor "strawberry" :cost 12}}
        honey {:insert-into [:honey]
               :values [{:flavor "lichi" :cost 120}]}]
    (is (= (pg/gen-query popsicles honey)
           {:with
            [[:hpop_aaa
              {:update :popsicles,
               :set {:flavor "strawberry", :cost 12},
               :returning [:*]}]
             [:hpop_aab
              {:insert-into [:honey],
               :values [{:flavor "lichi", :cost 120}],
               :returning [:*]}]],
            :select
            [[[:jsonb_build_object
               "popsicles"
               [:jsonb_build_object
                "update"
                {:select [[[:jsonb_agg [:to_jsonb :hpop_aaa]]]], :from :aaa}]
               "honey"
               [:jsonb_build_object
                "insert-into"
                {:select [[[:jsonb_agg [:to_jsonb :hpop_aab]]]], :from :aab}]]
              "results"]]})))
  (let [popsicles {:insert-into [:popsicles]
                   :values [{:flavor "strawberry" :cost 12}]}
        honey {:insert-into [:honey]
               :values [{:flavor "lichi" :cost 120}]}]
    (is (= (pg/gen-query popsicles honey)
           {:with
            [[:hpop_aaa
              {:insert-into [:popsicles],
               :values [{:flavor "strawberry", :cost 12}],
               :returning [:*]}]
             [:hpop_aab
              {:insert-into [:honey],
               :values [{:flavor "lichi", :cost 120}],
               :returning [:*]}]],
            :select
            [[[:jsonb_build_object
               "popsicles"
               [:jsonb_build_object
                "insert-into"
                {:select [[[:jsonb_agg [:to_jsonb :hpop_aaa]]]], :from :aaa}]
               "honey"
               [:jsonb_build_object
                "insert-into"
                {:select [[[:jsonb_agg [:to_jsonb :hpop_aab]]]], :from :aab}]]
              "results"]]}))))


(comment
(let [popsicles {:insert-into [:popsicles]
                   :values [{:flavor "strawberry" :cost 12}]
                   :returning [:*]}
        honey {:insert-into [:honey]
               :values [{:honey_type "lichi" :cost 120}]
               :returning [:*]}]
    (with-tx-rollback [tx conn]
      (jdbc-execute! tx (pg/gen-query popsicles honey) )))




(let [popsicles {:insert-into [:popsicles]
                   :values [{:flavor "strawberry" :cost 12}]}
        honey {:insert-into [:honey]
               :values [{:flavor "lichi" :cost 120}]}]
    (pg/gen-query popsicles honey))

(honey.sql/format {:select
 [[[:jsonb_build_object
    "popsicles"
    [:jsonb_build_object
     "insert-into"
     {:with
      [[:foo
        {:insert-into [:popsicles],
         :values [{:flavor "strawberry", :cost 12}],
         :returning [:*]}]],
      :select [[[:jsonb_agg [:to_jsonb :foo-]]]],
      :from [[:foo :foo-]]}]
    "honey"
    [:jsonb_build_object
     "insert-into"
     {:with
      [[:foo
        {:insert-into [:honey],
         :values [{:flavor "lichi", :cost 120}],
         :returning [:*]}]],
      :select [[[:jsonb_agg [:to_jsonb :foo-]]]],
      :from [[:foo :foo-]]}]]
   "results"]]} {:inline true})
  )
