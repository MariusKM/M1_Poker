package edu.sb.poker.util;

import javax.ws.rs.BadRequestException;
import javax.ws.rs.NotAuthorizedException;
import javax.ws.rs.WebApplicationException;


/**
 * Facade for JAX-RS based HTTP credentials creation. It offers adapters for the credential factory methods which map all
 * exceptions to {@link WebApplicationException}, suitable for a JAX-RS environment.
 */
@Copyright(year = 2016, holders = "Sascha Baumeister")
public class RestCredentials {

	/**
	 * Prevents external instantiation.
	 */
	private RestCredentials () {}


	/**
	 * Returns Basic credentials decoded from the given authentication.
	 * @param authentication an HTTP "authorization" header value
	 * @return the decoded HTTP Basic credentials
	 * @throws BadRequestException if the given authentication is malformed
	 * @throws NotAuthorizedException if the given authentication is {@code null} or the wrong type
	 */
	static public HttpCredentials.Basic newBasicInstance (final String authentication) throws BadRequestException, NotAuthorizedException {
		try {
			return HttpCredentials.newBasicInstance(authentication);
		} catch (final HttpCredentials.AuthenticationException exception) {
			throw new NotAuthorizedException("Basic");
		} catch (final IllegalArgumentException exception) {
			throw new BadRequestException();
		}
	}


	/**
	 * Returns Digest credentials decoded from the given authentication.
	 * @param authentication an HTTP "authorization" header value
	 * @return the decoded HTTP Digest credentials
	 * @throws BadRequestException if the given authentication is malformed
	 * @throws NotAuthorizedException if the given authentication is {@code null} or the wrong type
	 */
	static public HttpCredentials.Digest newDigestInstance (final String authentication) throws BadRequestException, NotAuthorizedException {
		try {
			return HttpCredentials.newDigestInstance(authentication);
		} catch (final HttpCredentials.AuthenticationException exception) {
			throw new NotAuthorizedException("Digest realm=\"\"");
		} catch (final IllegalArgumentException exception) {
			throw new BadRequestException();
		}
	}
}