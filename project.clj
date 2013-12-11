(defproject gmailtolabel "1.0"
  :description "GMail contacts to address labels printing GUI."
  :url "http://example.com/FIXME"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.5.1"]
                 [com.google.gdata/core "1.47.1"]
                 [seesaw "1.4.4"]]
  :aot :all
  :main gmailtolabel.gui)
