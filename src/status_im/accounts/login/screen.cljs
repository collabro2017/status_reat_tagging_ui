(ns status-im.accounts.login.screen
  (:require-macros [status-im.utils.views :refer [defview]])
  (:require [re-frame.core :refer [subscribe dispatch dispatch-sync]]
            [status-im.components.react :refer [view
                                                text
                                                text-input
                                                image
                                                linear-gradient
                                                touchable-highlight]]
            [status-im.components.status-bar :refer [status-bar]]
            [status-im.components.toolbar :refer [toolbar]]
            [status-im.components.text-field.view :refer [text-field]]
            [status-im.components.styles :refer [color-purple
                                                 color-white
                                                 icon-search
                                                 icon-back
                                                 icon-qr
                                                 toolbar-background1
                                                 toolbar-title-container
                                                 toolbar-title-text
                                                 button-input-container
                                                 button-input
                                                 white-form-text-input]]
            [status-im.qr-scanner.views.scan-button :refer [scan-button]]
            [status-im.i18n :refer [label]]
            [status-im.accounts.login.styles :as st]))

(defn toolbar-title [platform-specific]
  [view toolbar-title-container
   [text {:style             (merge toolbar-title-text {:color color-white})
          :platform-specific platform-specific
          :font              :medium}
    (label :t/login)]])

(defview address-input [address]
  [view
   [text-field
    {:value        address
     :editable     false
     :label        (label :t/address)
     :labelColor   "#ffffff80"
     :lineColor    :white
     :inputStyle   st/input-style
     :wrapperStyle (merge button-input st/address-input-wrapper)
     :onChangeText #(dispatch [:set-in [:login :address] %])}]])

(defview password-input [error]
  [view
   [text-field
    {:value        ""
     :error        (when (pos? (count error)) (label :t/wrong-password))
     :errorColor   :white
     :label        (label :t/password)
     :labelColor   "#ffffff80"
     :lineColor    :white
     :inputStyle   st/input-style
     :onChangeText #(do
                     (dispatch [:set-in [:login :password] %])
                     (dispatch [:set-in [:login :error] ""]))}]])

(defview login [{platform-specific :platform-specific}]
  [{:keys [address password error]} [:get :login]
   keyboard-height [:get :keyboard-height]]
  [view st/screen-container
   [linear-gradient {:colors    ["rgba(182, 116, 241, 1)" "rgba(107, 147, 231, 1)" "rgba(43, 171, 238, 1)"]
                     :start     [0, 0]
                     :end       [0.5, 1]
                     :locations [0, 0.8, 1]
                     :style     st/gradient-background}]
   [status-bar {:platform-specific platform-specific
                :type              :transparent}]
   [toolbar {:background-color :transparent
             :nav-action       {:image   {:source {:uri :icon_back_white}
                                          :style  icon-back}
                                :handler #(dispatch [:navigate-back])}
             :custom-content   [toolbar-title platform-specific]
             :action           {:image   {:style icon-search}
                                :handler #()}}]
   [view st/form-container
    [view st/form-container-inner
     [address-input (or address "")]
     [password-input error]]]
   [view st/bottom-actions-container
    [view st/connect-button-container
     [touchable-highlight
      {:on-press #(dispatch [:login-account address password])}
      [view st/connect-button
       [text {:style             st/connect-button-text
              :platform-specific platform-specific}
        (label :t/connect)]]]]]])
