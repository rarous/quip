(ns quip.client
  "A Quip API client library.

  For full API documentation, visit https://quip.com/api/.

  Typical usage:
        (let [opts {:access-token \"...\"}
              user @(quip/current-user opts)
              desktop @(quip/folders (get user \"desktop_folder_id\") opts)]
            (println \"There are\" (count (get desktop \"children\")) \"items on the desktop\"))

  In addition to standard getters and setters, we provide a few convenience
  methods for document editing. For example, you can use `add-to-first-list`
  to append items (in Markdown) to the first bulleted or checklist in a
  given document, which is useful for automating a task list."
  (:require [clojure.string :refer [join]]
            [cognitect.transit :as t]
            [org.httpkit.client :as http]))

(def ^:private endpoint "https://platform.quip.com/1")
(def ^:private blob-uri (str endpoint "/blob/"))

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

(defn- get-response [uri options callback]
  (let [ret (promise)]
    (http/get
     uri options
     (fn [{:keys [error body] :as resp}]
       (if-not error
         (deliver ret (callback resp))
         (deliver ret (read-json body)))))
    ret))

(defn- get-json-response [[resource id] options]
  (let [uri (join "/" [endpoint resource (ids id)])]
    (get-response
     uri options
     (fn [{:keys [body]}]
       (read-json body)))))

(defn blob
  "Retrieves the binary representation of the given blob from the given
  thread if the user has access.

  Supported option is :access-token. To generate a personal access
  token, visit this page: https://quip.com/api/personal-token.

  Returns promise of map with :status, :content-type and :stream keys.
  When an error occures the promise contains map with error details."
  [thread-id blob-id opts]
  (let [uri (str blob-uri thread-id "/" blob-id)
        options (options (:access-token opts))]
    (get-response
     uri options
     (fn [{:keys [status body headers]}]
       (let [content-type (:content-type headers)]
         {:content-type content-type
          :status status
          :stream body})))))

(defn threads
  "Returns the given threads. You may also pass in a 12 character URL
  suffix to get the permanent thread ID.

  Supported option is :access-token. To generate a personal access token,
  visit this page: https://quip.com/api/personal-token.

  Returns promise of map with parsed JSON result."
  [thread-id opts]
  (get-json-response ["threads" thread-id] (options (:access-token opts))))

(defn users
  "Returns the given users. If you use the multi-get variant of this method,
  we return data for each of the given users in a dictionary.
  You can provide either email addresses or user IDs in the `id` field;
  if you provide multiple email addresses for the same user we return multiple
  dictionary entries with the same user information for each.

  Supported option is :access-token. To generate a personal access token,
  visit this page: https://quip.com/api/personal-token.

  Returns promise of map with parsed JSON result."
  [user-id opts]
  (get-json-response ["users" user-id] (options (:access-token opts))))

(defn folders
  "Returns the given folders. If you use the multi-get varient of this method,
  we return data for each of the given folders in a dictionary by folder ID.

  Supported option is :access-token. To generate a personal access token,
  visit this page: https://quip.com/api/personal-token.

  Returns promise of map with parsed JSON result."
  [folder-id opts]
  (get-json-response ["folders" folder-id] (options (:access-token opts))))

(defn current-user
  "Returns the user corresponding to our :access-token.

  Supported option is :access-token. To generate a personal access token,
  visit this page: https://quip.com/api/personal-token.

  Returns promise of map with parsed JSON result."
  [opts] (users "current" opts))

(defn contacts
  "Returns a list of the contacts for the authenticated user.

  Supported option is :access-token. To generate a personal access token,
  visit this page: https://quip.com/api/personal-token.

  Returns promise of map with parsed JSON result."
  [opts] (users "contacts" opts))

(defn recent-threads
  "Returns the most recent threads to have received messages,
  similar to the inbox view in the Quip app.

  count - optional - The maximum number of results to return. Defaults to 10, with a maximum of 50.
  max-updated - optional - If given, we return threads updated before the given max-updated,
  which is a UNIX timestamp in microseconds. To use this argument for paging,
  you can use the :updated-usec field in the returned thread objects.

  Supported option is :access-token. To generate a personal access token,
  visit this page: https://quip.com/api/personal-token.

  Returns promise of map with parsed JSON result."
  [opts & {:keys [count max-updated]}]
  (threads (cond
            (and count max-updated)
            (str "recent?count=" count "&max_updated_usec=" max-updated)
            (some? count) (str "recent?count=" count)
            (some? max-updated) (str "recent?max_updated_usec=" max-updated)
            :else "recent")
           opts))
