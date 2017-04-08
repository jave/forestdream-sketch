(ns my-sketch.core
  (:require [quil.core :as q]
            [quil.middleware :as m]))

(defn myloadshape [shape]
  ;;atm im not sure how to create a reliable relative path, so an absolute path for now
   (q/load-shape (format "/home/joakim/forestdream-sketch/%s.svg" shape)))

(defn setup []
  (q/frame-rate 10)
  (q/color-mode :hsb)
  {:color 0
   :angle 0
   :z 0
   :imgz 0
   ;;im storing the tree svg here, not sure if good
   ;; load-shape works only in sketch-functions
   :tree (myloadshape "tree")
   :treedir 10
   })
;;(setup)
(defn update-state [state]

  (let
      [treedir (cond (= (q/key-as-keyword) :a) -10
                     (= (q/key-as-keyword) :s) 10
                     :else (:treedir state))
       
       showimg (cond (= (q/key-as-keyword) :1) (myloadshape "badhand") ;;TODO shapes should be loaded once in setup
                     (= (q/key-as-keyword) :2) (myloadshape "saw")
                     :else nil)
       imgz (if showimg (+ (:imgz state) 100) -2000) ;increment if showing a hand or something, orhterwise reset
;;       z (mod (+ (:z state) treedir) 15000)
       z (+ (:z state) treedir) 
       ]
  {
   ;;   :z (+ 1000 (mod (+ (:z state) 10) 1500))
   :treedir treedir
   :z  z
   :imgz imgz
   :tree (:tree state)
   :showimg showimg
   }))

(defn draw-tree [state x y z]
  (q/with-translation [x y z]
  (q/shape (:tree state)   )
;     (q/shape tree-shape   )
   )
  )

(defn draw-state [state]
  ;; Clear the sketch by filling it with light-grey color.
  (q/background 10)
  (q/perspective)
  (q/camera
   ;; eye x y z
   (+ (/ (q/width) 2.0)  (:z state))
;;  (+ (/ (q/height) 2.0)  (:z state))
   (/ (q/height) 2.0)
   (/ (q/height) 2.0)
;;   (/ (/ (q/height) 2.0) (Math/tan (/ (* Math/PI 60.0) 360.0)))

   ;; center x y z
   (+ (/ (q/width) 2.0)  (:z state))
   ;;                       (/ (q/width) 2.0)
   (/ (q/height) 2.0)
   0
   ;; up x y z
   0
   1
   0
   )

  ;; draw a grid of trees
  (doseq [a (range -5 5) b (range -5 5) :let [x (* 500 a) y (* -2000 b)] ]

    (draw-tree state x 100 y)
    )

  ;; a new camera, which is not moving
  (q/camera)
  ;; a debug text on screen
  (q/text   (format "key %s %s z: %s"(q/key-as-keyword) (q/key-pressed?) (:z state) ) 100 100)
  ;; this shows an image zooming by the viewer, trigger with keys 1,2 etc
  (if (:showimg state)
    (q/with-translation [0 0 (:imgz state)]
      (q/shape (:showimg state)   )
      ))
    )
  

(q/defsketch my-sketch
  :renderer :p3d
  :title "forest dream"
  :size [800 600]
  ; setup function called only once, during sketch initialization.
  :setup setup
  ; update-state is called on each iteration before draw-state.
  :update update-state
  :draw draw-state
  :features [:keep-on-top]
  ; This sketch uses functional-mode middleware.
  ; Check quil wiki for more info about middlewares and particularly
  ; fun-mode.

  ;; i wanted to try navigation, but that didnt work with svg shapes for some reason for me
  ;; 
  ;;:middleware [m/fun-mode m/navigation-3d]
  ;;
  :middleware [m/fun-mode ]
  )
