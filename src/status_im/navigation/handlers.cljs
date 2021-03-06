(ns status-im.navigation.handlers
  (:require [re-frame.core :refer [dispatch debug enrich after]]
            [status-im.utils.handlers :refer [register-handler]]))

(defn push-view [db view-id]
  (-> db
      (update :navigation-stack conj view-id)
      (assoc :view-id view-id)))

(defn replace-top-element [stack view-id]
  (let [stack' (if (pos? (count stack))
                 (pop stack)
                 stack)]
    (conj stack' view-id)))

(defn replace-view [db view-id]
  (-> db
      (update :navigation-stack replace-top-element view-id)
      (assoc :view-id view-id)))

(defmulti preload-data!
          (fn [db [_ view-id]] (or view-id (:view-id db))))

(defmethod preload-data! :default [db _] db)

(register-handler :navigate-to
  (enrich preload-data!)
  (fn [{:keys [view-id] :as db} [_ new-view-id]]
    (if (= view-id new-view-id)
      db
      (push-view db new-view-id))))

(register-handler :navigation-replace
  (enrich preload-data!)
  (fn [db [_ view-id]]
    (replace-view db view-id)))

(register-handler :navigate-back
  (enrich preload-data!)
  (fn [{:keys [navigation-stack] :as db} _]
    (if (>= 1 (count navigation-stack))
      db
      (let [[view-id :as navigation-stack'] (pop navigation-stack)]
        (-> db
            (assoc :view-id view-id)
            (assoc :navigation-stack navigation-stack'))))))

(register-handler :navigate-to-tab
  (enrich preload-data!)
  (fn [db [_ view-id]]
    (-> db
        (assoc :prev-tab-view-id (:view-id db))
        (replace-view view-id))))

(register-handler :on-navigated-to-tab
  (enrich preload-data!)
  (fn [db [_]]
    (assoc db :prev-tab-view-id nil)))

(register-handler :show-group-new
  (debug
    (fn [db _]
      (-> db
          (push-view :new-group)
          (assoc :new-group #{})
          (assoc :new-chat-name nil)))))

(register-handler :show-contacts
  (fn [db [_ click-handler]]
    (-> db
        (assoc :contacts-click-handler click-handler)
        (push-view :contact-list))))

(register-handler :remove-contacts-click-handler
                  (fn [db]
                    (dissoc db :contacts-click-handler)))

(register-handler :show-group-contacts
  (fn [db [_ group]]
    (-> db
        (assoc :contacts-group group)
        (push-view :group-contacts))))

(defn show-profile
  [db [_ identity]]
  (-> db
      (assoc :contact-identity identity)
      (push-view :profile)))

(register-handler :show-profile show-profile)

(defn show-profile-photo-capture
  [db [_ image-captured-fn]]
  (push-view db :profile-photo-capture))

(register-handler :show-profile-photo-capture show-profile-photo-capture)

(defn navigate-to-clean
  [db [_ view-id]]
  (-> db
      (assoc :navigation-stack (list))
      (push-view view-id)))

(register-handler :navigate-to-clean navigate-to-clean)
