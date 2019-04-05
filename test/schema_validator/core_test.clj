(ns schema-validator.core-test
  (:require [clojure.test :refer [deftest is testing]]
            [clojure.string :as str]
            [clojure.java.io :as io]
            [schema-validator.core :as sut]))

(def test-data-dir "test/test_data/")

(deftest test-ensure-leading-dot
  (is (= ".avsc" (sut/ensure-leading-dot "avsc")))
  (is (= ".avsc" (sut/ensure-leading-dot ".avsc"))))

(deftest test-parse-file-extensions
  (testing "simple cases"
    (doseq [[input output] [["avsc" #{".avsc"}]
                            [".avsc" #{".avsc"}]
                            ["json,avsc" #{".json" ".avsc"}]
                            ["json,.avsc" #{".json" ".avsc"}]
                            [".json,avsc" #{".json" ".avsc"}]
                            [".json,.avsc" #{".json" ".avsc"}]]]
      (is (= output (sut/parse-file-extensions input)))))
  (testing "complex cases"
    ;; note this is a highly unlikely scenario
    (doseq [[input output] [["foo.gz" #{".foo.gz"}]
                            [".bar.gz" #{".bar.gz"}]
                            ["foo.gz,bar.gz" #{".foo.gz" ".bar.gz"}]
                            ["foo.gz,.bar.gz" #{".foo.gz" ".bar.gz"}]
                            [".foo.gz,bar.gz" #{".foo.gz" ".bar.gz"}]
                            [".foo.gz,.bar.gz" #{".foo.gz" ".bar.gz"}]]]
      (is (= output (sut/parse-file-extensions input)))))
  (testing "throws AssertionError on bad inputs"
    (doseq [[input output] [[nil #{}]
                            ["" #{}]]]
      (is (thrown? AssertionError (sut/parse-file-extensions input))))))

(deftest test-get-proposed-schemas
  (testing "only attempts to parse supported file extensions"
    (is (.exists (io/file (str test-data-dir "placeholder_c.avsc")))
        "Test expects `placeholder_c.avsc` file to exist.")
    (is (= ["test/test_data/placeholder_a.json"
            "test/test_data/placeholder_b.json"]
           (sut/get-proposed-schemas test-data-dir #{".json"})))
    (is (= ["test/test_data/placeholder_a.json"
            "test/test_data/placeholder_b.json"
            "test/test_data/placeholder_c.avsc"]
           (sut/get-proposed-schemas test-data-dir #{".json" ".avsc"})))))

(deftest test-get-filename
  (is (= "placeholder_a" (sut/get-filename "test/test_data/placeholder_a.json"))))
