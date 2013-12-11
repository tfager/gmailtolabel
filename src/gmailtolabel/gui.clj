(ns gmailtolabel.gui
  (:use gmailtolabel.core)
  (:use seesaw.core)
  (:use seesaw.mig)
  (:use seesaw.font)
  (:use seesaw.graphics)
  (:import (java.awt.print PrinterJob Printable))
  (:gen-class))

(def win (frame :title "GMail to Label generator" :on-close :exit))
(def gmail (atom nil))
(def groups (atom []))
(def addresses (atom []))
(def current-page (atom 0))
(def sticker-config (atom { :sticker-width 100
                            :sticker-height 67
                            :stickers-x 2
                            :stickers-y 4
                            :font-size 4}))

(defn paint-one-label [ g { x :x
                           y :y
                           height :sticker-height
                           width :sticker-width
                           font-size :font-size }
                       address]
  (let [text-x (+ x (int (* width 0.2)))
        text-y (+ y (int (- (/ height 2.0) (* font-size 2.0))))]
    (.setFont g (font :size font-size))
    (.drawString g (first address) text-x text-y)
    (.drawString g (second address) text-x (+ text-y font-size))
    (.drawString g (str (get address 2) " " (get address 3)) text-x (+ text-y (* font-size 2)))))

(defn error-dialog [text]
  (-> (dialog :content (label text)
              :type :error :option-type :default)
    pack! show!))

(defn mm2pts [x]
  (* 2.8346456693 x))

(defn sticker-pages []
  (let [sy (:stickers-y @sticker-config)
        sx (:stickers-x @sticker-config)]
        (partition-all sy (partition sx sx (repeat 9 nil) @addresses))))

(defn paint-labels [g page-num]
  (let [sy (:stickers-y @sticker-config)
        sx (:stickers-x @sticker-config)
        pages (sticker-pages)
        page (nth pages page-num)
        width (:sticker-width @sticker-config)
        height (:sticker-height @sticker-config)
        font-size (:font-size @sticker-config)]
    (doall
      (map (fn [[ypos row]]
             (doall
               (map (fn [[xpos addr]]
                      (if (not (nil? addr))
                        (paint-one-label g {
                                   :x (* xpos width )
                                   :y (* ypos height)
                                   :sticker-height height
                                   :sticker-width width
                                   :font-size font-size }
                                            addr)))
                    (map-indexed vector row))))
             (map-indexed vector page)))))

(defn print-labels [e]
  (try
    (let [job (PrinterJob/getPrinterJob)
          pf (.defaultPage job)
          paper (.getPaper pf)]
      (.setPrintable job
          (proxy [java.awt.print.Printable] []
            (print [g pf page]
              (if (< page (count (sticker-pages)))
                (do 
                  (paint-labels (scale (translate g (.getImageableX pf) (.getImageableY pf)) (mm2pts 1)) page)
                  Printable/PAGE_EXISTS)
                Printable/NO_SUCH_PAGE))))
      (if (.printDialog job) (.print job)))
  (catch Exception e (error-dialog (str "Printing failed: " (.getMessage e))))))
    
(defn paint-preview [c g]
  (if (> (count @addresses) 0)
    (paint-labels g @current-page)))

(defn gui-get-groups [e]
  (try
    (let [username (config (select win [:#txt-username]) :text)
          password (config (select win [:#txt-password]) :text)
          listbox (select win [:#groups-list])
          service (create-service username password)
          group-data (get-groups service)]
      (swap! groups (constantly group-data))
      (swap! gmail (constantly service))
      (config! listbox :model (map second group-data)))
    (catch Exception e (error-dialog (str "Failed to get groups: " (.getMessage e))))))

(defn gui-set-page [delta event]
  (let [page-count (count (sticker-pages))
        new-page (+ @current-page delta)
        new-page (cond (>= new-page (dec page-count)) (dec page-count)
                       (< new-page 0) 0
                       :else new-page)]
    (swap! current-page (constantly new-page)) 
    (config! (select win [:#lbl-page-info]) :text (str "Page " (inc @current-page) " of " page-count))
    (config! (select win [:#prev-page-button]) :enabled? (< 0 new-page))
    (config! (select win [:#next-page-button]) :enabled? (< new-page (dec page-count)))
    (repaint! (select win [:#canvas]))))

(defn gui-get-addresses [e]
  (try
    (let [group-name (selection (select win [:#groups-list]))
          group-id (first (first (filter #(= group-name (second %)) @groups)))
          contacts (get-contacts @gmail group-id)
          custom-name-field (config (select win [:#txt-name-field]) :text)
          address-list (map #(format-contact % custom-name-field) contacts)]
      (swap! addresses (constantly address-list))
      (swap! current-page (constantly 0))
      (gui-set-page 0 nil))
  (catch Exception e (error-dialog (str "Failed to get addresses: " (.getMessage e)))))) 

(defn gui-update-sticker-config [e]
  (let [newconfig (reduce
                    (fn [prop-map prop]
                      (assoc prop-map (keyword prop) (read-string (config (select win [(keyword (str "#txt-" prop))]) :text))))
                    {}
                    ["stickers-x" "stickers-y" "sticker-width" "sticker-height" "font-size"])]
    (swap! sticker-config (constantly newconfig))
    (repaint! (select win [:#canvas]))))
                 
(defn init-gui []
  (let [bold-font (font :style :bold)
        lbl-gmail (label :text "GMail credentials" :font bold-font)
        lbl-username (label "Username:")
        txt-username (text :id :txt-username)
        lbl-password (label "Password:")
        txt-password (password :id :txt-password)
        get-groups-button (button :id :get-groups-button :text "Get groups" :listen [:action gui-get-groups])
        lbl-group (label "Select group:")
        groups-list (combobox :id :groups-list)
        get-addresses-button (button :id :get-addresses-button :text "Get addresses" :listen [:action gui-get-addresses])
        lbl-name-field (label "Custom name field:")
        txt-name-field (text :id :txt-name-field)
        lbl-sticker-config (label :font bold-font :text "Sticker settings")
        lbl-stickers-x (label "Stickers horiz.:")
        txt-stickers-x (text :id :txt-stickers-x :text (:stickers-x @sticker-config))
        lbl-stickers-y (label "Stickers vert.:")
        txt-stickers-y (text :id :txt-stickers-y :text (:stickers-y @sticker-config))
        lbl-sticker-width (label "Sticker width mm:")
        txt-sticker-width (text :id :txt-sticker-width :text (:sticker-width @sticker-config))
        lbl-sticker-height (label "Sticker height mm:")
        txt-sticker-height (text :id :txt-sticker-height :text (:sticker-height @sticker-config))
        lbl-font-size (label "Font size mm:")
        txt-font-size (text :id :txt-font-size :text (:font-size @sticker-config))
        update-button (button :text "Update" :listen [:action gui-update-sticker-config])
        canvas (xyz-panel :id :canvas :paint paint-preview :size [ 210 :by 297 ] :border 1)
        prev-page-button (button :id :prev-page-button :text "<-" :listen [:action (partial gui-set-page -1)] :enabled? false)
        lbl-page-info (label :text "No addresses" :id :lbl-page-info :h-text-position :center :halign :center)
        next-page-button (button :id :next-page-button :text "->" :listen [:action (partial gui-set-page +1)] :enabled? false)
        print-button (button :id :print-button :text "Print" :listen [:action print-labels])
        main-panel (mig-panel :constraints ""
                              :items [[ lbl-gmail "wrap" ]
                                      [ lbl-username "" ]
                                      [ txt-username "width 100, wrap" ]
                                      [ lbl-password "" ]
                                      [ txt-password "width 100" ]
                                      [ get-groups-button "wrap" ]
                                      [ lbl-group "" ]
                                      [ groups-list "width 250" ]
                                      [ get-addresses-button "wrap" ]
                                      [ lbl-name-field "" ]
                                      [ txt-name-field "width 150, wrap" ]
                                      [ lbl-sticker-config "span 2" ]
                                      [ canvas "span 3 5, wrap, grow"]
                                      [ lbl-stickers-x ""]
                                      [ txt-stickers-x "width 30, wrap"]
                                      [ lbl-stickers-y ""]
                                      [ txt-stickers-y "width 30, wrap"]
                                      [ lbl-sticker-width ""]
                                      [ txt-sticker-width "width 30, wrap"]
                                      [ lbl-sticker-height ""]
                                      [ txt-sticker-height "width 30, wrap"]
                                      [ lbl-font-size ""]
                                      [ txt-font-size "width 30, wrap"]
                                      [ update-button "" ]
                                      [ print-button ""]
                                      [ prev-page-button "" ]
                                      [ lbl-page-info "growx"]
                                      [ next-page-button ""]
                                      ]
                              ) ]
    (config! win :content main-panel)
    (-> win pack! show!)))
        
(defn -main [& args]
  (invoke-later (init-gui)))


