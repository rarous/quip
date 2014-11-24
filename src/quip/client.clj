(ns quip.client
  (:require [clojure.string :refer [join]]
            [clojure.walk :refer [keywordize-keys]]
            [cognitect.transit :as t]
            [org.httpkit.client :as http])
  (:import [java.io ByteArrayInputStream ByteArrayOutputStream]))

(def ^:private endpoint "https://platform.quip.com/1")
(def ^:private threads-uri (str endpoint "/threads/"))
(def ^:private blob-uri (str endpoint "/blob/"))
(def ^:private message-uri (str endpoint "/messages/"))

(defn- options [access-token]
  {:as :stream
   :keepalive 30000
   :headers {"Authorization" (str "Bearer " access-token)}})
(defn- reader [in] (t/reader in :json-verbose))
(defn- read-json [stream] (t/read (reader stream)))
(defn- ids [in]
  (if (sequential? in)
    (str "?ids=" (join "," in))
    in))

(defn blob
  "Retrieves the binary representation of the given blob from the given
  thread if the user has access.

  Supported option is :access-token. To generate a personal access
  token, visit this page: https://quip.com/api/personal-token.

  Returns promise of map with :status, :content-type and :stream keys.
  When an error occures the promise contains map with error details."
  [thread-id blob-id opts]
  (let [uri (str blob-uri thread-id "/" blob-id)
        options (options (:access-token opts))
        ret (promise)]
    (http/get
     uri options
     (fn [{:keys [status error body headers]}]
       (if-not error
         (let [content-type (:content-type headers)]
           (deliver ret {:content-type content-type
                         :status status
                         :stream body}))
         (deliver ret (keywordize-keys (read-json body))))))
    ret))

(defn threads
  "Returns the given threads. You may also pass in a 12 character URL
  suffix to get the permanent thread ID.

  Supported option is :access-token. To generate a personal access
  token, visit this page: https://quip.com/api/personal-token.

  Returns promise of map with parsed JSON result."
  [thread-id opts]
  (let [uri (str threads-uri (ids thread-id))
        options (options (:access-token opts))
        ret (promise)]
    (http/get
     uri options
     (fn [{:keys [body]}]
       (if (sequential? thread-id)
         (deliver ret (into {} (map (fn [[k v]] [k (keywordize-keys v)])) (read-json body)))
         (deliver ret (keywordize-keys (read-json body))))))
    ret))
