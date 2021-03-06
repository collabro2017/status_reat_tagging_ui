(ns status-im.chat.views.message
  (:require-macros [status-im.utils.views :refer [defview]])
  (:require [clojure.string :as s]
            [re-frame.core :refer [subscribe dispatch]]
            [reagent.core :as r]
            [status-im.i18n :refer [message-status-label]]
            [status-im.components.react :refer [view
                                                text
                                                image
                                                animated-view
                                                touchable-highlight]]
            [status-im.components.animation :as anim]
            [status-im.chat.views.request-message :refer [message-content-command-request]]
            [status-im.chat.styles.message :as st]
            [status-im.models.commands :refer [parse-command-message-content
                                               parse-command-request]]
            [status-im.resources :as res]
            [status-im.utils.datetime :as time]
            [status-im.constants :refer [text-content-type
                                         content-type-status
                                         content-type-command
                                         content-type-command-request]]
            [status-im.utils.logging :as log]))

(defn message-date [timestamp platform-specific]
  [view {}
   [view st/message-date-container
    [text {:style             st/message-date-text
           :platform-specific platform-specific
           :font              :default}
     (time/to-short-str timestamp)]]])

(defn contact-photo [{:keys [photo-path]}]
  [view st/contact-photo-container
   [image {:source (if (s/blank? photo-path)
                     res/user-no-photo
                     {:uri photo-path})
           :style  st/contact-photo}]])

(defn contact-online [{:keys [online]}]
  (when online
    [view st/online-container
     [view st/online-dot-left]
     [view st/online-dot-right]]))

(defn message-content-status [{:keys [from content]} platform-specific]
  [view st/status-container
   [view st/status-image-view
    [contact-photo {}]
    [contact-online {:online true}]]
   [text {:style             st/status-from
          :platform-specific platform-specific
          :font              :default}
    from]
   [text {:style             st/status-text
          :platform-specific platform-specific
          :font              :default}
    content]])

(defn message-content-audio [{:keys [platform-specific]}]
  [view st/audio-container
   [view st/play-view
    [image {:source res/play
            :style  st/play-image}]]
   [view st/track-container
    [view st/track]
    [view st/track-mark]
    [text {:style             st/track-duration-text
           :platform-specific platform-specific
           :font              :default}
     "03:39"]]])

(defview message-content-command [content preview platform-specific]
  [commands [:get-commands-and-responses]]
  (let [{:keys [command content]} (parse-command-message-content commands content)
        {:keys [name icon type]} command]
    [view st/content-command-view
     [view st/command-container
      [view (st/command-view command)
       [text {:style             st/command-name
              :platform-specific platform-specific
              :font              :default}
        (str (if (= :command type) "!" "") name)]]]
     (when icon
       [view st/command-image-view
        [image {:source {:uri icon}
                :style  st/command-image}]])
     (if preview
       preview
       [text {:style             st/command-text
              :platform-specific platform-specific
              :font              :default}
        (str content)])]))

(defn set-chat-command [message-id command]
  (dispatch [:set-response-chat-command message-id (keyword (:name command))]))

(defn message-view
  [message content]
  [view (st/message-view message)
   #_(when incoming-group
       [text {:style message-author-text}
        "Justas"])
   content])

(defmulti message-content (fn [_ message _]
                            (message :content-type)))

(defmethod message-content content-type-command-request
  [wrapper message platform-specific]
  [wrapper message [message-content-command-request message platform-specific] platform-specific])

(defn text-message
  [{:keys [content] :as message} platform-specific]
  [message-view message
   [text {:style             (st/text-message message)
          :platform-specific platform-specific
          :font              :default}
    (str content)]])

(defmethod message-content text-content-type
  [wrapper message platform-specific]
  [wrapper message [text-message message platform-specific] platform-specific])

(defmethod message-content content-type-status
  [_ message platform-specific]
  [message-content-status message platform-specific])

(defmethod message-content content-type-command
  [wrapper {:keys [content rendered-preview] :as message} platform-specific]
  [wrapper message
   [message-view message [message-content-command content rendered-preview platform-specific]]
   platform-specific])

(defmethod message-content :default
  [wrapper {:keys [content-type content] :as message} platform-specific]
  [wrapper message
   [message-view message
    [message-content-audio {:content           content
                            :content-type      content-type
                            :platform-specific platform-specific}]]
   platform-specific])

(defview message-delivery-status [{:keys [delivery-status chat-id message-id] :as message} platform-specific]
  [status [:get-in [:message-status chat-id message-id]]]
  [view st/delivery-view
   [image {:source (case (or status delivery-status)
                     :seen {:uri :icon_ok_small}
                     :seen-by-everyone {:uri :icon_ok_small}
                     :failed res/delivery-failed-icon
                     nil)
           :style  st/delivery-image}]
   [text {:style             st/delivery-text
          :platform-specific platform-specific
          :font              :default}
    (message-status-label (or status delivery-status))]])

(defview member-photo [from]
  [photo-path [:photo-path from]]
  [view st/photo-view
   [image {:source (if (s/blank? photo-path)
                     res/user-no-photo
                     {:uri photo-path})
           :style  st/photo}]])

(defn incoming-group-message-body
  [{:keys [selected same-author from] :as message} content platform-specific]
  (let [delivery-status :seen-by-everyone]
    [view st/group-message-wrapper
     (when selected
       [text {:style             st/selected-message
              :platform-specific platform-specific
              :font              :default}
        "Mar 7th, 15:22"])
     [view (st/incoming-group-message-body-st message)
      [view st/message-author
       (when (not same-author) [member-photo from])]
      [view st/group-message-view
       content
       ;; TODO show for last or selected
       (when (and selected delivery-status)
         [message-delivery-status {:delivery-status delivery-status} platform-specific])]]]))

(defn message-body
  [{:keys [outgoing] :as message} content platform-specific]
  [view (st/message-body message)
   content
   (when outgoing
     [message-delivery-status message platform-specific])])

(defn message-container-animation-logic [{:keys [to-value val callback]}]
  (fn [_]
    (let [to-value @to-value]
      (when (< 0 to-value)
        (anim/start
          (anim/spring val {:toValue  to-value
                            :friction 4
                            :tension  10})
          (fn [arg]
            (when (.-finished arg)
              (callback))))))))

(defn message-container [message & children]
  (if (:new? message)
    (let [layout-height (r/atom 0)
          anim-value (anim/create-value 1)
          anim-callback #(dispatch [:set-message-shown message])
          context {:to-value layout-height
                   :val      anim-value
                   :callback anim-callback}
          on-update (message-container-animation-logic context)]
      (r/create-class
        {:component-did-update
         on-update
         :reagent-render
         (fn [message & children]
           @layout-height
           [animated-view {:style (st/message-container anim-value)}
            (into [view {:onLayout (fn [event]
                                     (let [height (.. event -nativeEvent -layout -height)]
                                       (reset! layout-height height)))}]
                  children)])}))
    (into [view] children)))

(defn chat-message [{:keys [outgoing delivery-status timestamp new-day group-chat message-id chat-id]
                     :as   message}
                    platform-specific]
  (let [status (subscribe [:get-in [:message-status chat-id message-id]])]
    (r/create-class
      {:component-did-mount
       (fn []
         (when (and (not outgoing)
                    (not= :seen delivery-status)
                    (not= :seen @status))
           (dispatch [:send-seen! chat-id message-id])))
       :reagent-render
       (fn [{:keys [outgoing delivery-status timestamp new-day group-chat]
             :as   message}
            platform-specific]
         [message-container message
          ;; TODO there is no new-day info in message
          (when new-day
            [message-date timestamp platform-specific])
          [view
           (let [incoming-group (and group-chat (not outgoing))]
             [message-content
              (if incoming-group
                incoming-group-message-body
                message-body)
              (merge message {:delivery-status (keyword delivery-status)
                              :incoming-group  incoming-group})
              platform-specific])]])})))
