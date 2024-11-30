(ns cissy.dbms.dialect
  (:require [babashka.pods :as pod]))


(pod/load-pod 'org.babashka/go-sqlite3 "0.2.3")
