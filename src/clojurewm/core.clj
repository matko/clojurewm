(ns clojurewm.core
  (:import [com.sun.jna.platform.unix X11 X11$XEvent]))

;; Copied from http://nakkaya.com/2009/11/16/java-native-access-from-clojure/
(defmacro jna-call [lib func ret & args]
  "Call a function through JNA without using an interface"
  `(let [library#  (name ~lib)
         function# (com.sun.jna.Function/getFunction library# ~func)] 
     (.invoke function# ~ret (to-array [~@args]))))

(def x11 X11/INSTANCE)

(def ^:dynamic display)

(defn grab-key [key-name mod-mask]
  (. x11 XGrabKey display
     (. x11 XKeysymToKeycode display
        (. x11 XStringToKeysym key-name))
     mod-mask
     (. x11 DefaultRootWindow display)
     true
     X11/GrabModeAsync
     X11/GrabModeAsync))

(defn grab-button [button-num mod-mask]
  (. x11 XGrabButton display button-num mod-mask
     (. x11 (DefaultRootWindow display))
     true
     (bit-or X11/ButtonPressMask
             X11/ButtonReleaseMask
             X11/PointerMotionMask)
     X11/GrabModeAsync X11/GrabModeAsync X11/None X11/None))

(defn server []
  (binding [display (. x11 XOpenDisplay "0")]
    (try
      (grab-key "F1" X11/Mod1Mask)
      (grab-button 1 X11/Mod1Mask)
      (grab-button 3 X11/Mod1Mask)
      (let [event (X11$XEvent.)]
        (loop []
          (. x11 (XNextEvent display event))
          
          (cond
           (and (= (. event type)
                   X11/KeyPress)
                (not= (.. event xkey subwindow)
                      X11/None))
           (do
             (. x11 (XRaiseWindow display (.. event xkey subwindow)))))
           
          (recur)))
      
      (finally
       (. x11 XCloseDisplay display)))))


(defn -main
  "tinywm clone"
  [& args]
  (server))
