package com.baasbox.android.internal;

import com.baasbox.android.BAASBoxClientException;
import com.baasbox.android.BAASBoxConnectionException;
import com.baasbox.android.BAASBoxInvalidSessionException;
import com.baasbox.android.BAASBoxServerException;

public interface OnLogoutHelper {

	public void retryLogin() throws BAASBoxInvalidSessionException,
			BAASBoxClientException, BAASBoxServerException,
			BAASBoxConnectionException;

	public void onLogout();

}
