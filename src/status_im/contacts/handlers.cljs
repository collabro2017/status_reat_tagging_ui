(ns status-im.contacts.handlers
  (:require [re-frame.core :refer [after dispatch]]
            [status-im.utils.handlers :refer [register-handler]]
            [status-im.models.contacts :as contacts]
            [status-im.utils.crypt :refer [encrypt]]
            [clojure.string :as s]
            [status-im.protocol.api :as api]
            [status-im.utils.utils :refer [http-post]]
            [status-im.utils.phone-number :refer [format-phone-number]]
            [status-im.utils.handlers :as u]
            [status-im.utils.utils :refer [require]]
            [status-im.utils.logging :as log]))

(defn save-contact
  [_ [_ contact]]
  (contacts/save-contacts [contact]))

(defn watch-contact
  [_ [_ contact]]
  (api/watch-user contact))

(register-handler :watch-contact (u/side-effect! watch-contact))

(register-handler :update-contact
  (-> (fn [db [_ {:keys [whisper-identity] :as contact}]]
        (update-in db [:contacts whisper-identity] merge contact))
      ((after save-contact))))

(defn load-contacts! [db _]
  (let [contacts (->> (contacts/get-contacts)
                      (map (fn [{:keys [whisper-identity] :as contact}]
                             [whisper-identity contact]))
                      (into {}))]
    (doseq [[_ contact] contacts]
      (dispatch [:watch-contact contact]))
    (assoc db :contacts contacts)))

(register-handler :load-contacts load-contacts!)

;; TODO see https://github.com/rt2zz/react-native-contacts/issues/45
(def react-native-contacts (require "react-native-contacts"))

(defn contact-name [contact]
  (->> contact
       ((juxt :givenName :middleName :familyName))
       (remove s/blank?)
       (s/join " ")))

(defn normalize-phone-contacts [contacts]
  (let [contacts' (js->clj contacts :keywordize-keys true)]
    (map (fn [{:keys [thumbnailPath phoneNumbers] :as contact}]
           {:name          (contact-name contact)
            :photo-path    thumbnailPath
            :phone-numbers phoneNumbers}) contacts')))

(defn fetch-contacts-from-phone!
  [_ _]
  (.getAll react-native-contacts
           (fn [error contacts]
             (if error
               (dispatch [:error-on-fetching-loading error])
               (let [contacts' (normalize-phone-contacts contacts)]
                 (dispatch [:get-contacts-identities contacts']))))))

(register-handler :sync-contacts
  (u/side-effect! fetch-contacts-from-phone!))

(defn get-contacts-by-hash [contacts]
  (->> contacts
       (mapcat (fn [{:keys [phone-numbers] :as contact}]
                 (map (fn [{:keys [number]}]
                        (let [number' (format-phone-number number)]
                          [(encrypt number')
                           (-> contact
                               (assoc :phone-number number')
                               (dissoc :phone-numbers))]))
                      phone-numbers)))
       (into {})))

(defn add-identity [contacts-by-hash contacts]
  (map (fn [{:keys [phone-number-hash whisper-identity address]}]
         (let [contact (contacts-by-hash phone-number-hash)]
           (assoc contact :whisper-identity whisper-identity
                          :address address)))
       (js->clj contacts)))

(defn request-stored-contacts [contacts]
  (let [contacts-by-hash (get-contacts-by-hash contacts)
        data             (or (keys contacts-by-hash) ())]
    (http-post "get-contacts" {:phone-number-hashes data}
               (fn [{:keys [contacts]}]
                 (let [contacts' (add-identity contacts-by-hash contacts)]
                   (dispatch [:add-contacts contacts']))))))

(defn get-identities-by-contacts! [_ [_ contacts]]
  (request-stored-contacts contacts))

(register-handler :get-contacts-identities
  (u/side-effect! get-identities-by-contacts!))

(defn save-contacts! [{:keys [new-contacts]} _]
  (contacts/save-contacts new-contacts))

(defn add-new-contacts
  [{:keys [contacts] :as db} [_ new-contacts]]
  (let [identities    (set (map :whisper-identity contacts))
        new-contacts' (->> new-contacts
                           (remove #(identities (:whisper-identity %)))
                           (map #(vector (:whisper-identity %) %))
                           (into {}))]
    (-> db
        (update :contacts merge new-contacts')
        (assoc :new-contacts (vals new-contacts')))))

(register-handler :add-contacts
  (after save-contacts!)
  add-new-contacts)

(defn add-new-contact [db [_ {:keys [whisper-identity] :as contact}]]
  (-> db
      (update :contacts assoc whisper-identity contact)
      (assoc :new-contact-identity "")))

(register-handler :add-new-contact
   (u/side-effect!
     (fn [_ [_ {:keys [whisper-identity] :as contact}]]
       (when-not (contacts/get-contact whisper-identity)
         (dispatch [::new-contact contact])))))

(register-handler ::new-contact
  (-> add-new-contact
      ((after save-contact))
      ((after watch-contact))))

(defn set-contact-identity-from-qr
  [db [_ _ contact-identity]]
  (assoc db :new-contact-identity contact-identity))

(register-handler :set-contact-identity-from-qr set-contact-identity-from-qr)

(register-handler :contact-update-received
  (u/side-effect!
    ;; TODO: security issue: we should use `from` instead of `public-key` here, but for testing it is much easier to use `public-key`
    (fn [db [_ from {{:keys [public-key last-updated name] :as account} :account}]]
      (let [prev-last-updated (get-in db [:contacts public-key :last-updated])]
        (if (< prev-last-updated last-updated)
          (let [contact (-> (assoc account :whisper-identity public-key)
                            (dissoc :public-key))]
            (dispatch [:update-contact contact])
            (dispatch [:update-chat! public-key {:name name}])))))))

(register-handler :contact-online-received
  (u/side-effect!
    (fn [db [_ from {last-online :at :as payload}]]
      (let [prev-last-online (get-in db [:contacts from :last-online])]
        (if (< prev-last-online last-online)
          (dispatch [:update-contact {:whisper-identity from
                                      :last-online      last-online}]))))))
