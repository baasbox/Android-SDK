/*
 * Copyright (C) 2014. BaasBox
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions andlimitations under the License.
 */

package com.baasbox.android;

/**
 * Interface definition for a callback to be invoked when new data is available
 * in an asynchronous streaming request.
 * <p>This interface is used to handle the asynchronous streaming of files.</p>
 * <p/>
 * <p>The callback is invoked on a background thread ant it may be invoked many times while the data is streamed
 * from the server.<br/>
 * While the content is downloaded the data byte array will never be null and will contain
 * new data received from the server.</p>
 * <p>
 * When the streaming of the file ends, the callback will be invoked one more time with
 * a null <code>data</code> byte array, that is an indication that the content has been completely
 * retrieved from the server.
 * </p>
 * <p>
 * The return value of the last call to {@link #onData(byte[], int, long, String, String)} is what will be presented
 * to the handler, wrapped in a {@link com.baasbox.android.BaasResult}, if during any invocation an exception is thrown
 * it will be wrapped as a {@link com.baasbox.android.BaasResult#failure(BaasException)}.
 * </p>
 * <p>
 * This is an example of streaming a response to a String:
 * <p/>
 * <pre>
 *     <code>
 *         public class StreamToString implements DataStreamHandler&lt;String&gt;{
 *              private ByteArrayOutputStream out = new ByteArrayOutputStream();
 *
 *             &#64;Override
 *              public String onData(byte[] data,int read,long contentLength,String id,String contentType) throws Exception{
 *                  if(data!=null){
 *                      out.write(data,0,read);
 *                     return null;
 *                  } else{
 *                      return out.toString("UTF-8");
 *                  }
 *              }
 *         }
 *     </code>
 * </pre>
 * </p>
 *
 * @param <R> the type of response that this stream handler will deliver
 * @author Andrea Tortorella
 * @since 0.7.3
 */
public interface DataStreamHandler<R> {
// -------------------------- OTHER METHODS --------------------------

    /**
     * Method invoked right before data starts to stream.
     * @param id the identifier to which this stream is bound to
     * @param contentLength the length of the body
     * @param contentType the contentType of the response
     * @throws Exception
     */
    void startData(String id,long contentLength,String contentType) throws Exception;

    /**
     * Method invoked when new data is available.
     *
     * @param data          a byte[] array filled with new available data or null if there is no more available.
     * @param read          the number of actual bytes that can be read from <code>data</code>
     * @throws Exception any exception thrown will be wrapped in a {@link com.baasbox.android.BaasException}
     */
    void onData(byte[] data, int read) throws Exception;

    /**
     * Method invoked when the whole data has been streamed
     *
     * @param id the identifier to which this stream is bound to
     * @param contentLength the length of the body
     * @param contentType the contentType of the response
     * @return an object
     * @throws Exception
     */
    R endData(String id,long contentLength,String contentType) throws Exception;

    void finishStream(String id);
}
