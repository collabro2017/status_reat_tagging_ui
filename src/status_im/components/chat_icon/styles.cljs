(ns status-im.components.chat-icon.styles
  (:require [status-im.components.styles :refer [font
                                                 title-font
                                                 color-white
                                                 chat-background
                                                 online-color
                                                 selected-message-color
                                                 separator-color
                                                 text1-color
                                                 text2-color
                                                 toolbar-background1]]))

(defn default-chat-icon [color]
  {:margin          4
   :width           36
   :height          36
   :alignItems      :center
   :justifyContent  :center
   :borderRadius    18
   :backgroundColor color})

(defn default-chat-icon-chat-list [color]
  (merge (default-chat-icon color)
         {:width  40
          :height 40}))

(defn default-chat-icon-menu-item [color]
  (merge (default-chat-icon color)
         {:width  24
          :height 24}))

(defn default-chat-icon-profile [color]
  (merge (default-chat-icon color)
         {:width  64
          :height 64}))

(def default-chat-icon-text
  {:marginTop  -2
   :color      color-white
   :fontFamily font
   :fontSize   16
   :lineHeight 20})

(def chat-icon
  {:margin       4
   :borderRadius 18
   :width        36
   :height       36})

(def chat-icon-chat-list
  (merge chat-icon
         {:width  40
          :height 40}))

(def chat-icon-menu-item
  (merge chat-icon
         {:width  24
          :height 24}))

(def chat-icon-profile
  (merge chat-icon
         {:width         64
          :height        64
          :border-radius 32}))

(def online-view
  {:position        :absolute
   :bottom          0
   :right           0
   :width           20
   :height          20
   :borderRadius    10
   :backgroundColor online-color
   :borderWidth     2
   :borderColor     color-white})

(def online-view-menu-item
  (merge online-view
         {:width         14
          :height        14
          :border-radius 7}))

(def online-view-profile
  (merge online-view
         {:width         24
          :height        24
          :border-radius 12}))

(def online-dot
  {:position        :absolute
   :top             6
   :width           4
   :height          4
   :borderRadius    2
   :backgroundColor color-white})
(def online-dot-left (merge online-dot {:left 3}))
(def online-dot-right (merge online-dot {:left 9}))

(def photo-pencil
  {:margin-left 6
   :margin-top  3
   :font-size   12
   :color       :white})

(def online-dot-menu-item
  (merge online-dot
         {:top    4
          :width  3
          :height 3}))
(def online-dot-left-menu-item
  (merge online-dot-menu-item {:left 1.7}))
(def online-dot-right-menu-item
  (merge online-dot-menu-item {:left 6.3}))

(def online-dot-profile
  (merge online-dot
         {:top    8
          :width  4
          :height 4}))
(def online-dot-left-profile
  (merge online-dot-profile {:left 5}))
(def online-dot-right-profile
  (merge online-dot-profile {:left 11}))

(def container
  {:width  44
   :height 44})

(def container-chat-list
  {:width  48
   :height 48})

(def container-menu-item
  {:width  32
   :height 32})

(def container-profile
  {:width  72
   :height 72})
