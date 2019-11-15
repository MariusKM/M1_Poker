package edu.sb.poker.service;

import static edu.sb.poker.persistence.Person.Group.ADMIN;
import static edu.sb.poker.service.BasicAuthenticationFilter.REQUESTER_IDENTITY;
import static javax.ws.rs.core.MediaType.APPLICATION_JSON;
import static javax.ws.rs.core.MediaType.APPLICATION_XML;
import static javax.ws.rs.core.MediaType.TEXT_PLAIN;
import static javax.ws.rs.core.Response.Status.CONFLICT;
import static javax.ws.rs.core.Response.Status.FORBIDDEN;
import static javax.ws.rs.core.Response.Status.NOT_FOUND;
import static javax.ws.rs.core.Response.Status.BAD_REQUEST;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;
import javax.persistence.EntityManager;
import javax.persistence.RollbackException;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
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
import javax.ws.rs.QueryParam;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import org.eclipse.persistence.oxm.MediaType;
import edu.sb.poker.persistence.BaseEntity;
import edu.sb.poker.persistence.Person;
import edu.sb.poker.util.HashCodes;
import edu.sb.poker.util.RestJpaLifecycleProvider;

@Path("persons")
public class PersonService {

	@GET
	@Produces({
			APPLICATION_JSON, APPLICATION_XML
	})
	public List<Person> queryEntity(@QueryParam("forename") String forename, @QueryParam("surname") String surname,
			@QueryParam("email") String email, @QueryParam("resultOffset") int resultOffset,
			@QueryParam("resultLimit") int resultLimit, @QueryParam("nullOffer") boolean nullOffer,
			@QueryParam("nullAnswer") boolean nullAnswer) {

		final EntityManager eM = RestJpaLifecycleProvider.entityManager("poker");

		String queryString = "SELECT id from Person as p WHERE" + "(:forename is null or p.forename = :forename)"
				+ "AND (:surname is null or p.surname = :surname)" + "AND (:email is null or p.email = :email) ";

		if (nullOffer) {
			queryString += " AND (p.negotiationOffer is null)";
		}

		if (nullAnswer) {
			queryString += " AND (p.negotiationAnswer is null)";
		}

		if (resultOffset != 0) {
			queryString += " Offset " + resultOffset;
		}

		if (resultOffset != 0) {
			queryString += " Offset " + resultOffset;
		}

		List<Person> personProxies = eM.createQuery(queryString, Person.class).setParameter("forename", forename)
				.setParameter("surname", surname).setParameter("email", email).getResultList();
		List<Person> persons = new ArrayList<Person>();
		for (Person proxy : personProxies) {

			persons.add(eM.find(Person.class, proxy.getIdentity()));
		}

		return persons.stream()
				.sorted(Comparator.comparing(p -> ((Person) p).getName()).thenComparing(p -> ((Person) p).getEmail()))
				.collect(Collectors.toList());
	}

	@POST
	@Consumes(APPLICATION_JSON)
	@Produces(TEXT_PLAIN)
	public long postPerson(@HeaderParam(REQUESTER_IDENTITY) @Positive final long requesterIdentity,
			@NotNull @NotEmpty final Person input, @QueryParam("avatarReference") long avatarReference,
			@HeaderParam("SET-PASSWORD") long setPassword) {

		final EntityManager pokerManager = RestJpaLifecycleProvider.entityManager("poker");
		final Person requester = pokerManager.find(Person.class, requesterIdentity);
		if (requester.getGroup() != ADMIN && input.getGroup() == ADMIN)
			throw new ClientErrorException(FORBIDDEN);

		final String passHash = HashCodes.sha2HashText(256, input.getPasswordHash());

		Person person;

		if (input.getIdentity() == 0) {
			person = input;
			person.setPasswordHash(passHash);
			pokerManager.persist(person);
		} else {
			final String DOCUMENT_QUERY = "Update Person" + "SET personIdentity = :personIdentity "
					+ "avatarReference = :avatarReference " + "pokerTableReference = :pokerTableReference"
					+ "email = :email" + "balance = :balance" + "position = :position" + "title = :title"
					+ "surname = :surname" + "forename = :forename" + "street = :street" + "postcode = :postcode"
					+ "city = :city" + "country = :country" + "negotiationOffer = :negotiationOffer"
					+ "WHERE personIdentity = :personIdentity";
			person = pokerManager.createQuery(DOCUMENT_QUERY, Person.class)
					.setParameter("personIdentity", input.getIdentity())
					.setParameter("avatarReference", avatarReference != 0 ? avatarReference : 1)
					.setParameter("pokerTableReference", input.getPoker().getIdentity())
					.setParameter("email", input.getEmail()).setParameter("balance", input.getBalance())
					.setParameter("position", input.getPosition()).setParameter("title", input.getName().getTitle())
					.setParameter("surname", input.getName().getFamily())
					.setParameter("forename", input.getName().getGiven())
					.setParameter("street", input.getAddress().getStreet())
					.setParameter("postcode", input.getAddress().getPostcode())
					.setParameter("city", input.getAddress().getCity())
					.setParameter("country", input.getAddress().getCountry())
					.setParameter("negotiationOffer", input.getNegotiation().getOffer()).getSingleResult();
			person.setGroup(input.getGroup());
			person.setPasswordHash(passHash);
			person.setVersion(input.getVersion());
			pokerManager.flush();
		}

		try {
			pokerManager.getTransaction().commit();
		} catch (final RollbackException exception) {
			throw new ClientErrorException(CONFLICT);
		} finally {
			pokerManager.getTransaction().begin();
		}

		return person.getIdentity();
	}

	@GET
	@Path("{id}")
	public Person getPersonById(@HeaderParam(REQUESTER_IDENTITY) @Positive final long requesterIdentity,
			@PathParam("id") @Positive final long identity) {
		final EntityManager messengerManager = RestJpaLifecycleProvider.entityManager("poker");

		if (identity == 0) {
			final Person requester = messengerManager.find(Person.class, requesterIdentity);

			return requester;
		}

		Person person = messengerManager.find(Person.class, identity);
		if (person == null) {
			throw new ClientErrorException(NOT_FOUND);
		}

		return person;
	}

}
