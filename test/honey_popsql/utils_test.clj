(ns honey-popsql.utils-test
  (:require
    [clojure.test :refer [deftest testing is]]
    [honey-popsql.utils :refer [extract-table-name agg-queries-for-table]]))

(deftest test-extract-table-name
  (testing "for select query"
    (is (= (extract-table-name
             {:select [:*]
              :from [:foo]})
           :foo))
    (is (= (extract-table-name
             {:select [:*]
              :from [[:foo]]})
           :foo))

    (is (= (extract-table-name
             {:select [:*]
              :from :foo})
           :foo)))

  (testing "for insert query"
    (is (= (extract-table-name
             {:insert-into :foo})
           :foo)))

  (testing "for update query"
    (is  (= (extract-table-name
              {:update :foo})
            :foo)))

  (testing "delete query"
    (is (= (extract-table-name
             {:delete-from :foo})
           :foo))))


#_(deftest test-agg-queries-for-table
  (testing "One query per table"
    (is (= (agg-queries-for-table {:select [:*]
                                   :from [:foo]})
           {:foo [{:select [:*]
                   :from [:foo]}]}))
    (is (= (agg-queries-for-table {:select [:*]
                                   :from [:foo]}
                                  {:select [:*]
                                   :from [:foo-bar]})
           {:foo [{:select [:*], :from [:foo]}],
            :foo-bar [{:select [:*], :from [:foo-bar]}]}))
    (is (= (agg-queries-for-table {:select [:*]
                                   :from [:foo]}
                                  {:select [:*]
                                   :from [:foo-bar]}
                                  {:select [:*]
                                   :from [:bar]})
           {:foo [{:select [:*], :from [:foo]}],
            :foo-bar [{:select [:*], :from [:foo-bar]}],
            :bar [{:select [:*], :from [:bar]}]})))

  (testing "Multiple queries per table"
    (is (= (agg-queries-for-table {:select [:*]
                                   :from [:foo]}
                                  {:update :foo
                                   :set [:= :a 1]})
           {:foo [{:select [:*], :from [:foo]}
                  {:update :foo, :set [:= :a 1]}]}))

    (is (= (agg-queries-for-table {:select [:*]
                                   :from [:foo]}
                                  {:update :foo
                                   :set [:= :a 1]}
                                  {:select [:*]
                                   :from [:bar]})

           {:foo [{:select [:*], :from [:foo]} 
                  {:update :foo, :set [:= :a 1]}],
            :bar [{:select [:*], :from [:bar]}]}))))
