(ns my-sketch.core
  (:require [quil.core :as q]
            [quil.middleware :as m]
            [clojure.core.matrix :as x]
            ))

(defn myloadshape1 [shape]
  ;;atm im not sure how to create a reliable relative path, but just having the images in project root seems to work
  (q/load-shape (format "%s.svg" shape)))

(def myloadshape (memoize myloadshape1))

(def tree (ref nil))

(defn v+ [v1 v2]
  ;;vector addition, im not sure what the best way is in processing
  ;;(into [] (map + v1 v2))
  (x/add v1 v2);;use core.matrix instead of my homegrown thing
  )

(def thingstack (atom nil))
(defn dropthing [thing coords]
  (swap! thingstack conj {:thing thing :coords coords}))
;;(dropthing "tree" [0  (q/height) 0])
;;(dropthing "tree" [0  360 0])
;;@thingstack
(defn setup []
  (q/frame-rate 30)
  (q/color-mode :hsb)
  (dosync (ref-set tree    (myloadshape "see_tree")))
  (dropthing "tree" [0  (q/height) 0]) ;;just to debug the thing stack, drop a thing at origo
  {:color 0
   :angle 0
   :cameraxyz [0 0 0]
   :imgz 0
   ;;   :cameramovement [0 0 -10]
      :cameramovement [0 0 0] ;;start standing still
   :cameraangle 0.0
   :debug 2
   })


(defn update-state [state]
  (let
      [cameramovement (cond (= (q/key-as-keyword)  :down) [0 0 -10]
                            (= (q/key-as-keyword) :up) [0 0  10]
                            (= (q/key-as-keyword) :right) [-10 0 0]
                            (= (q/key-as-keyword) :left) [10  0 0]
                            (= (q/key-as-keyword) :z) [0  0 0]                                          
                            :else nil)
       cameramovement (if cameramovement
                        (x/mmul cameramovement 1);;you can scale the speed of movement
                        (:cameramovement state))
       ;;shapes are memoized, so they should be loaded once only
       showimg (cond
                 ;;(>  (:imgz state) 0) nil
                 (= (q/key-as-keyword) :1) (myloadshape "badhand")
                 (= (q/key-as-keyword) :2) (myloadshape "saw")

                 :else nil)
       imgz (if showimg (+ (:imgz state) 100) -2000) ;increment if showing a sprite, orhterwise reset
       cameraxyz  (cond (= (q/key-as-keyword) :r) [0 0 0]
                        :else
                        (v+ (:cameraxyz state) cameramovement))
       cameraangle (cond (and  (q/key-pressed?)(= (q/key-as-keyword) :v)) (+ (:cameraangle state) 0.01)
                         (and (q/key-pressed?)(= (q/key-as-keyword) :b)) (+ (:cameraangle state) -0.01)
                         (and (q/key-pressed?)(= (q/key-as-keyword) :t)) 0
                         :else (:cameraangle state))
       ]
    ;;drop a thing at camera position (needs a toggle code lide :debug)
    (if (= (q/key-as-keyword) :d)
      (do (dropthing "tree" cameraxyz))
      )
    {
     :cameramovement cameramovement
     :cameraangle cameraangle
     :cameraxyz  cameraxyz
     :imgz imgz
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
    )
  )

(defn draw-state [state]
  ;; Clear the sketch by filling it with light-grey color.
  (q/background 20)
  (q/perspective)
  (let
      [cxyz (v+ [(/ (q/width) 2.0) (/ (q/height) -1) 0]
                (:cameraxyz state))]
    
    (q/begin-camera)
    
    (q/camera)
    (q/rotate-y (:cameraangle state))
    (q/translate
     (nth cxyz 0)
     (nth cxyz 1)
     (nth cxyz 2)
     )

    (q/end-camera)

    (q/push-matrix)
    (q/reset-matrix)

    ;; a flashlight pointng in the view direction
    (q/spot-light [2, 2, 150]
                  [0 0 0];;[640 720 0];;cxyz
                  ;;       (v+ cxyz [0 0 -100])
                  [0 0 -1]
                  (/ Math/PI 8), 32)

    ;;another light following the mouse
    (q/spot-light 102, 153, 204,
                  (q/mouse-x), (q/mouse-y), 0,
                  0, 0, -1,
                  (/ Math/PI 2), 600)

                                        ;      (q/directional-light 51, 102, 126, -1, 0, 0)
;;   (q/ambient-light 0 0 33    0 00 0 )

    (q/pop-matrix)
    
    ;; draw a grid of trees
    (doseq [a (range -10 10) b (range -10 10)
            :let [x (* 500 a)
                  y (q/height)
                  z (* -2000 b)] ]

      (draw-tree state x y z)
      )

    ;;took me a while figuring out to use "doall" because "map" is lazy
    (doall (map (fn [item]
           (q/with-translation (:coords item)
             (q/shape      (myloadshape (:thing item))   )
             ))
           @thingstack)  )


    
    ;; a new camera, which is not moving
    (q/camera)
    (q/no-lights)
    ;; a debug text on screen
    (if    (= 2 (:debug state))
      (q/text   (format "s:%s d:%s key %s %s z: %s w:%s h:%s cxyz:%s cxyz:%s imgz:%s ca:%s"
                        @thingstack
                        (:debug state) (q/key-as-keyword) (q/key-pressed?)
                        (:cameramovement state) (q/width) (q/height) (:cameraxyz state) cxyz (:imgz state) (:cameraangle state))
                
                200 200))

    ;; this shows an image zooming by the viewer, trigger with keys 1,2 etc
    (if (:showimg state)
      (q/with-translation [(/(q/height)2)
                           -400;;(/(q/width)2)
                           (:imgz state)]
        (q/shape (:showimg state)   )
        ))
    )
  ;; (q/save-frame) ;; used when recording a movie from still frames
  ;;(q/exit) ;;when using virtualgl, i need to terminate with quit rather than cloing the applet window(this was fixed upstream)
  )


(q/defsketch my-sketch
  :renderer :p3d
  :title "forest dream"
  ;;  :size [800 600]
  :size [(/ 1280 2) (/ 720 2)]
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
