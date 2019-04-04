(ns schema-validator.core-test
  (:require [clojure.test :refer [deftest is testing]]
            [clojure.string :as str]
            [clojure.java.io :as io]
            [schema-validator.core :as sut]))

(def test-data-dir "test/test_data/")

(deftest test-get-proposed-schemas
  (testing "only attempts to parse files ending in `.json`"
    (is (.exists (io/file (str test-data-dir "placeholder_c.avdl")))
        "Test expects placeholder .avdl file to be present in dir")
    (is (= ["test/test_data/placeholder_a.json"
            "test/test_data/placeholder_b.json"]
           (sut/get-proposed-schemas test-data-dir)))))

(deftest test-get-filename
  (is (= "placeholder_a" (sut/get-filename "test/test_data/placeholder_a.json"))))
