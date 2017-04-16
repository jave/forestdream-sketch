(ns my-sketch.core
  (:require [quil.core :as q]
            [quil.middleware :as m]))

(defn myloadshape [shape]
  ;;atm im not sure how to create a reliable relative path, but just having the images in project root seems to work
  (q/load-shape (format "%s.svg" shape)))

(def tree (ref nil))

(defn v+ [v1 v2]
  ;;vector addition, im not sure what the best way is in processing
  (into [] (map + v1 v2))
  )

(defn setup []
  (q/frame-rate 30)
  (q/color-mode :hsb)
  (dosync (ref-set tree    (myloadshape "see_tree")))
  {:color 0
   :angle 0
   :cameraxyz [0 0 0]
   :imgz 0
   ;;im storing the tree svg here, not sure if good
   ;; load-shape works only in sketch-functions
                                        ;   :tree (myloadshape "tree")
   ;;      :tree (myloadshape "see_tree")
   :cameramovement [0 0 -10]
   :cameraangle 0.0
   :debug 0
   })

(defn update-state [state]

  (let
      [cameramovement (cond (= (q/key-as-keyword) :w) [0 0 -10]
                            (= (q/key-as-keyword) :s) [0 0  10]
                            (= (q/key-as-keyword) :a) [-10 0 0]
                            (= (q/key-as-keyword) :d) [10  0 0]
                            (= (q/key-as-keyword) :z) [0  0 0]                                          
                            :else (:cameramovement state))

       ;;TODO shapes should be loaded once in setup
       showimg (cond
                 ;;(>  (:imgz state) 0) nil
                 (= (q/key-as-keyword) :1) (myloadshape "badhand")
                 (= (q/key-as-keyword) :2) (myloadshape "saw")

                 :else nil)
       imgz (if showimg (+ (:imgz state) 100) -2000) ;increment if showing a sprite, orhterwise reset
       ;;z (+ (:z state) cameramovement)
       cameraxyz  (cond (= (q/key-as-keyword) :r) [0 0 0]
                        :else
                        (v+ (:cameraxyz state) cameramovement))
       cameraangle (cond (and  (q/key-pressed?)(= (q/key-as-keyword) :v)) (+ (:cameraangle state) 0.01)
                         (and (q/key-pressed?)(= (q/key-as-keyword) :b)) (+ (:cameraangle state) -0.01)
                         :else (:cameraangle state))
       ]
    {
     ;;   :z (+ 1000 (mod (+ (:z state) 10) 1500))
     :cameramovement cameramovement
     :cameraangle cameraangle
     :cameraxyz  cameraxyz
     :imgz imgz
                                        ;   :tree (:tree state)
     :showimg showimg

     :debug (cond (and (= (:debug state) 0) (q/key-pressed?) (= (q/key-as-keyword) :p)) 1
                  (and (= (:debug state) 1) (not (q/key-pressed?)) (= (q/key-as-keyword) :p)) 2
                  (and (= (:debug state) 2) (q/key-pressed?) (= (q/key-as-keyword) :p)) 3
                  (and (= (:debug state) 3) (not (q/key-pressed?)) (= (q/key-as-keyword) :p)) 0
                  
                  :else (:debug state)
                  )}))

  (defn draw-tree [state x y z]
    (q/with-translation [x y z]
      (q/shape @tree   )
                                        ;     (q/shape tree-shape   )
      )
    )

  (defn draw-state [state]
    ;; Clear the sketch by filling it with light-grey color.
    (q/background 20)
    ;;  (q/lights)
    (q/perspective)
    (let
        [cxyz (v+ [(/ (q/width) 2.0) (* 2 (q/height)) 0]
                  (:cameraxyz state))]
      (q/begin-camera)
      

      (q/camera
       ;; eye x y z
       (nth cxyz 0)
       (nth cxyz 1)
       (nth cxyz 2)
       ;;    (+ (/ (q/width) 2.0) 0); (:z state))
       ;; ;;  (+ (/ (q/height) 2.0)  (:z state))
       ;;    (* 2 (q/height));1000;(/ (q/height) 2.0)
       ;;      (:z state);0;(/ (q/height) 2.0)
       ;; ;;   (/ (/ (q/height) 2.0) (Math/tan (/ (* Math/PI 60.0) 360.0)))


       
       ;; center x y z
       0;600000;(+ (/ (q/width) 2.0)  (:z state))
       ;;                       (/ (q/width) 2.0)
       0;(/ (q/height) 2.0)
       -1000000
       ;; up x y z
       0
       1
       0
       )
      (q/rotate-y (:cameraangle state))

      (q/end-camera)

      ;; draw a grid of trees
      (doseq [a (range -10 10) b (range -10 10)
              :let [x (* 500 a)
                    y (q/height)
                    z (* -2000 b)] ]

        (draw-tree state x y z)
        )

      ;; a new camera, which is not moving
      (q/camera)
      ;; a debug text on screen
      (if    (= 2 (:debug state))
      (q/text   (format " d:%s key %s %s z: %s w:%s h:%s cxyz:%s cxyz:%s imgz:%s ca:%s"
                        (:debug state) (q/key-as-keyword) (q/key-pressed?)
                        (:cameramovement state) (q/width) (q/height) (:cameraxyz state) cxyz (:imgz state) (:cameraangle state))

                20 20))

      ;; this shows an image zooming by the viewer, trigger with keys 1,2 etc
      (if (:showimg state)
        (q/with-translation [(/(q/height)2)
                             -400;;(/(q/width)2)
                             (:imgz state)]
          (q/shape (:showimg state)   )
          ))
      )
    ;; (q/save-frame) ;; used when recording a movie from still frames
    ;;(q/exit) ;;when using virtualgl, i need to terminate with quit rather than cloing the applet window
    )


  (q/defsketch my-sketch
    :renderer :p3d
    :title "forest dream"
    ;;  :size [800 600]
    :size [(/ 1280 1) (/ 720 1)]
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

  ;;
