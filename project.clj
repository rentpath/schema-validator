(defproject com.rentpath/schema-validator "0.4.0"
  :description "FIXME: write description"
  :url "http://example.com/FIXME"
  :license {:name "MIT"
            :url "https://github.com/rentpath/schema-validator/blob/master/LICENSE"}
  :main schema-validator.core
  :aot [schema-validator.core]
  :uberjar-name "schema-validator.jar"
  :dependencies [[cheshire "5.8.1"]
                 [org.clojure/clojure "1.8.0"]
                 [org.clojure/tools.cli "0.4.1"]
                 [org.apache.avro/avro "1.8.1"]
                 [com.hotels/avro-compatibility "2.1.1"]
                 [com.damballa/abracad "0.4.13"]
                 [clj-http "3.9.1"]
                 [org.slf4j/slf4j-nop "1.7.25"]])
