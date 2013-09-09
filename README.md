Android-SDK
===========

This version of the BAASBox SDK allows to manage the documents (i.e. objects) by creating, updating, deleting and 
retrieving their JSON data.

Methods do not directly throw exceptions or return the result, but they wrap the status of the method 
execution inside an instance of the class BAASBoxResult. 
If an exception has been thrown during the method execution, 
the subsequent request of the method BAASBoxResult.get() will throw that exception, 
if the method ended successfully, the plain result will be returned instead.

This structural choice has been made with the Android *AsyncTask* class specification in mind. 
You can continue building Android Apps in the same way you always do.

Here's an example of how you could combine AsyncTask and BAASBox.


     public class GetDocumentTask extends AsyncTask<String, Void, BAASBoxResult<JSONObject>> {
 
        @Override
        protected BAASBoxResult<JSONObject> doInBackground(String... params) {
                String collection = params[0];
                String id = params[1];
 
                return box.getDocument(collection, id);
        }
 
        @Override
        protected void onPostExecute(BAASBoxResult<JSONObject> result) {
                try {
                        JSONObject obj = result.get();
                        onDocumentReceived(obj);
                } catch (BAASBoxInvalidSessionException e) {
                        showLoginActivity();
                } catch (BAASBoxException e) {
                        onError(e);
                }
        }
     }
