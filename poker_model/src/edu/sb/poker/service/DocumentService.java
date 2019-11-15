package edu.sb.poker.service;

import static javax.ws.rs.core.MediaType.TEXT_PLAIN;
import static javax.ws.rs.core.MediaType.WILDCARD;
import static javax.ws.rs.core.Response.Status.CONFLICT;
import static javax.ws.rs.core.Response.Status.NOT_ACCEPTABLE;
import static javax.ws.rs.core.Response.Status.NOT_FOUND;
import java.io.IOException;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceException;
import javax.persistence.RollbackException;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Positive;
import javax.ws.rs.ClientErrorException;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Response;
import edu.sb.poker.persistence.Document;
import edu.sb.poker.util.Copyright;
import edu.sb.poker.util.HashCodes;
import edu.sb.poker.util.RestJpaLifecycleProvider;


/**
 * JAX-RS based REST service implementation for polymorphic entity resources, defining the following path and method combinations:
 * <ul>
 * <li><b>GET documents/{id}</b>: Returns the content of the document with the given identity.</li>
 * <li><b>POST documents</b>: Creates or updates a document from content.</li>
 * </ul>
 */
@Path("documents")
@Copyright(year = 2005, holders = "Sascha Baumeister")
public class DocumentService {
	static private final String HEADER_ACCEPT = "Accept";
	static private final String HEADER_CONTENT_TYPE = "Content-Type";
	static private final String DOCUMENT_QUERY = "select d from Document as d where d.hash = :hash";

	@POST
	@Consumes(WILDCARD)
	@Produces(TEXT_PLAIN)
	public long modifyDocument (
		@NotNull @NotEmpty final byte[] content,
		@HeaderParam(HEADER_CONTENT_TYPE) @NotEmpty final String contentType
	) {
		final EntityManager pokerManager = RestJpaLifecycleProvider.entityManager("poker");
		final String contentHash = HashCodes.sha2HashText(256, content);

		final Document document = pokerManager.createQuery(DOCUMENT_QUERY, Document.class)
			.setParameter("hash", contentHash)
			.getResultList()
			.stream()
			.findAny()
			.orElse(new Document(content));
		document.setType(contentType);

		if (document.getIdentity() == 0) {
			pokerManager.persist(document);
		} else {
			pokerManager.flush();
		}

		try {
			pokerManager.getTransaction().commit();
		} catch (final RollbackException exception) {
			throw new ClientErrorException(CONFLICT);
		} finally {
			pokerManager.getTransaction().begin();
		}

		return document.getIdentity();
	}


	/**
	 * Returns the document's content with the given identity.
	 * @param documentIdentity the document identity
	 * @return the matching content (HTTP 200)
	 * @throws ClientErrorException (HTTP 404) if the given document cannot be found
	 * @throws ClientErrorException (HTTP 415) if the given document cannot be compressed due to it's format
	 * @throws PersistenceException (HTTP 500) if there is a problem with the persistence layer
	 * @throws IllegalStateException (HTTP 500) if the entity manager associated with the current thread is not open
	 */
	@GET
	@Path("{id}")
	@Produces(WILDCARD)
	public Response findDocument (
		@PathParam("id") @Positive final long documentIdentity,
		@HeaderParam(HEADER_ACCEPT) @NotNull @NotEmpty final String accept
	) throws IOException {
		final EntityManager pokerManager = RestJpaLifecycleProvider.entityManager("poker");
		final Document document = pokerManager.find(Document.class, documentIdentity);
		if (document == null) throw new ClientErrorException(NOT_FOUND);

		final String groupType = document.getType().trim().split("/")[0] + "/*";
		final Set<String> acceptTypes = Stream.of(accept.split(",")).map(type -> type.split(";")[0]).collect(Collectors.toSet());
		if (!acceptTypes.contains(WILDCARD) & !acceptTypes.contains(groupType) & !acceptTypes.contains(document.getType())) throw new ClientErrorException(NOT_ACCEPTABLE);

		return Response.ok(document.getContent(), document.getType()).build();
	}
}