/**
 * Provides classes necessary to use the BaasBox API,
 * and the main entry point to configure the client.
 * <br/>
 * A BaasBox connected app needs to initialize the
 * client through the {@link com.baasbox.android.BaasBox} class
 * providing an optional configuration.
 * <br/>
 * Once this initialization has been done, the server functionality
 * may be accessed through wrapper classes for each kind of resource
 * exposed via the rest api.
 * <br/>
 * Each rest endpoint is accessible through asynchronous methods,
 * that give a callback interface using {@link com.baasbox.android.BaasHandler}
 * general interface,
 * Asyncrhonous calls may be canceled or suspended for later resumption, through
 * the handle {@link com.baasbox.android.RequestToken} returned by asynchronous
 * calls.
 * <br/>
 * The api also provides synchronous alternatives using the
 * <em>sync</em> version of the methods, mostly for cases when the
 * request is initiated by another thread other than the main one.
 * <br/>
 * Different resources are wrapped by specific classes:
 * <ul>
 *     <li>{@link com.baasbox.android.BaasUser} a user of the BaasBox server</li>
 *     <li>{@link com.baasbox.android.BaasDocument} a json document stored in a collectiont on the server</li>
 *     <li>{@link com.baasbox.android.BaasFile} a blob stored on the server</li>
 * </ul>
 *
 * Since remote calls done through http, may fail results are always wrapped in an
 * option result type {@link com.baasbox.android.BaasResult}
 *
 */
package com.baasbox.android;