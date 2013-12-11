(ns gmailtolabel.core
  (:import (com.google.gdata.client.contacts ContactsService ContactQuery))
  (:import (com.google.gdata.data.contacts ContactFeed ContactGroupFeed))
  (:import (java.net URL)))

(defn create-service [username password]
  "Creates a Google Contacts API Service."
  (doto (ContactsService. "GmailToLabels")
        (.setUserCredentials username password)))

(defn get-groups [service]
  "Returns ID/Name pairs of all groups found in the google account."
  (let [feedUrl (URL. "https://www.google.com/m8/feeds/groups/default/full")]
    (->> (.getFeed service feedUrl ContactGroupFeed)
      .getEntries
      (map (fn [entry] [(.getId entry) (-> entry .getTitle .getPlainText)]))))) 
    
(defn get-contacts [service group]
  "Returns ContactEntry objects of all contacts in group 'group' (AtomID of the group)"
  (let [feedUrl (URL. "https://www.google.com/m8/feeds/contacts/default/full")
        query (doto (ContactQuery. feedUrl)
                (.setGroup group))]
    (.getEntries (.query service query ContactFeed))))

(defn get-user-defined-field [entry field-name]
  (if-not (or (nil? field-name) (= 0 (.length field-name)))
    (if-let [fields (seq (.getUserDefinedFields entry))]
      (if-let [entries (filter #(= field-name (.getKey %)) fields)]
        (.getValue (first entries))))))

(defn format-contact
  ([c] (format-contact c nil))
  ([c custom-name-field]
    [ (or (get-user-defined-field c custom-name-field) (-> c .getName .getFullName .getValue))
      (-> (first (-> c .getStructuredPostalAddresses)) .getStreet .getValue)
      (-> (first (-> c .getStructuredPostalAddresses)) .getPostcode .getValue)
      (-> (first (-> c .getStructuredPostalAddresses)) .getCity .getValue) ]))

