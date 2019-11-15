package edu.sb.poker.service;

import static edu.sb.poker.service.BasicAuthenticationFilter.REQUESTER_IDENTITY;
import static javax.ws.rs.core.MediaType.APPLICATION_JSON;
import static javax.ws.rs.core.MediaType.APPLICATION_XML;
import static javax.ws.rs.core.MediaType.TEXT_PLAIN;
import static javax.ws.rs.core.Response.Status.NOT_FOUND;
import static javax.ws.rs.core.Response.Status.CONFLICT;
import static javax.ws.rs.core.Response.Status.FORBIDDEN;
import static javax.ws.rs.core.Response.Status.BAD_REQUEST;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import javax.persistence.Cache;
import javax.persistence.EntityManager;
import javax.persistence.RollbackException;
import javax.persistence.Table;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Positive;
import javax.ws.rs.ClientErrorException;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;

import edu.sb.poker.persistence.Document;
import edu.sb.poker.persistence.Game;
import edu.sb.poker.persistence.Hand;
import edu.sb.poker.persistence.Person;
import edu.sb.poker.persistence.PokerTable;
import edu.sb.poker.util.RestJpaLifecycleProvider;

@Path("tables")
public class TableService {
	@GET
	@Produces({
			APPLICATION_JSON, APPLICATION_XML
	})
	public List<PokerTable> getTables(@QueryParam("alias") String alias, @QueryParam("avatar") Document avatar,
			@QueryParam("resultOffset") int resultOffset, @QueryParam("resultLimit") int resultLimit) {

		final EntityManager eM = RestJpaLifecycleProvider.entityManager("poker");

		String queryString = "SELECT id from PokerTable as p WHERE" + "(:alias is null or p.alias = :alias)"
				+ "AND (:avatarReference is null or p.avatarReference = :avatarReference)";

		if (resultOffset != 0) {
			queryString += " Offset " + resultOffset;
		}

		if (resultOffset != 0) {
			queryString += " Offset " + resultOffset;
		}

		List<PokerTable> tableProxies = eM.createQuery(queryString, PokerTable.class).setParameter("alias", alias)
				.setParameter("avatarReference", avatar.getIdentity()).getResultList();
		List<PokerTable> table = new ArrayList<PokerTable>();
		for (PokerTable t : tableProxies) {
			table.add(eM.find(PokerTable.class, t.getIdentity()));
		}

		return table.stream().sorted(Comparator.comparing(t -> ((PokerTable) t).getAlias()))
				.collect(Collectors.toList());
	}

	@POST
	@Produces(TEXT_PLAIN)
	public long postTable(@HeaderParam(REQUESTER_IDENTITY) @Positive final long requesterIdentity,
			@NotNull @NotEmpty final PokerTable input) {
		final EntityManager pokerManager = RestJpaLifecycleProvider.entityManager("poker");
		final Person requester = pokerManager.find(Person.class, requesterIdentity);
		if (requester.getGroup() != Person.Group.ADMIN)
			throw new ClientErrorException(FORBIDDEN);

		PokerTable pokerTable;

		if (input.getIdentity() == 0) {
			pokerTable = input;
			pokerManager.persist(pokerTable);
		} else {
			final String DOCUMENT_QUERY = "UPDATE PokerTable " + "SET pokerTableIdentity = :pokerTableIdentity"
					+ "avatarReference = :avatarReference"
					+ "alias = :alias WHERE pokerTableIdentity = :pokerTableIdentity";
			pokerTable = pokerManager.createQuery(DOCUMENT_QUERY, PokerTable.class)
					.setParameter("pokerTableIdentity", input.getIdentity())
					.setParameter("avatarReference", input.getDocument().getIdentity())
					.setParameter("alias", input.getAlias()).getSingleResult();
			pokerManager.flush();
		}

		try {
			pokerManager.getTransaction().commit();
		} catch (final RollbackException exception) {
			throw new ClientErrorException(CONFLICT);
		} finally {
			pokerManager.getTransaction().begin();
		}

		return pokerTable.getIdentity();
	}

	@GET
	@Path("{id}")
	public PokerTable getPersonById(@HeaderParam(REQUESTER_IDENTITY) @Positive final long requesterIdentity,
			@PathParam("id") @Positive final long identity) {
		final EntityManager messengerManager = RestJpaLifecycleProvider.entityManager("poker");

		PokerTable table = messengerManager.find(PokerTable.class, identity);
		if (table == null) {
			throw new ClientErrorException(NOT_FOUND);
		}

		return table;
	}

	@PUT
	@Path("{id}/players/{pos}")
	@Produces(TEXT_PLAIN)
	public String putPersonToTable(@HeaderParam(REQUESTER_IDENTITY) @Positive final long requesterIdentity,
			@PathParam("id") @Positive final long identity, @PathParam("pos") @Positive final Byte position) {

		final EntityManager pokerManager = RestJpaLifecycleProvider.entityManager("poker");
		final Person requester = pokerManager.find(Person.class, requesterIdentity);
		final PokerTable pokerTable = pokerManager.find(PokerTable.class, identity);
		// if requester is an admin
		if (requester.getGroup() == Person.Group.ADMIN || requester.getPoker().getIdentity() == pokerTable.getIdentity()
				|| pokerTable.getPlayers().contains(requester)) {
			throw new ClientErrorException(BAD_REQUEST);
		}

		// prüft für jeden spieler ob die position schon belegt wurde.
		for (Person p : pokerTable.getPlayers()) {
			if (p.getPosition() == position) {
				throw new ClientErrorException(BAD_REQUEST);
			}
		}

		pokerTable.getPlayers().add(requester);

		final String DOCUMENT_QUERY = "UPDATE Person "
				+ "SET position = :position WHERE personIdentity = :personIdentity";
		Person updatedRequester = pokerManager.createQuery(DOCUMENT_QUERY, Person.class)
				.setParameter("personIdentity", requester.getIdentity()).getSingleResult();

		try {
			pokerManager.getTransaction().commit();
		} catch (final RollbackException exception) {
			throw new ClientErrorException(CONFLICT);
		} finally {
			pokerManager.getTransaction().begin();
		}

		Cache cache = pokerManager.getEntityManagerFactory().getCache();
		(cache).evict(updatedRequester.getClass(), updatedRequester.getIdentity());

		return updatedRequester.getPosition().toString();
	}

	@DELETE
	@Path("{id}/players/{pos}")
	@Produces(TEXT_PLAIN)
	public String deletePersonFromTable(@HeaderParam(REQUESTER_IDENTITY) @Positive final long requesterIdentity,
			@PathParam("id") @Positive final long identity, @PathParam("pos") @Positive final Byte position) {

		final EntityManager pokerManager = RestJpaLifecycleProvider.entityManager("poker");
		final Person requester = pokerManager.find(Person.class, requesterIdentity);
		final PokerTable pokerTable = pokerManager.find(PokerTable.class, identity);
		final Person personToBeRemoved = pokerManager
				.createQuery("SELECT * FROM PERSON WHERE position = :position", Person.class)
				.setParameter("position", position).getSingleResult();

		// the poker spot is already empty
		if (personToBeRemoved == null) {
			throw new ClientErrorException(BAD_REQUEST);

		}

		if (personToBeRemoved.getIdentity() != requester.getIdentity() && requester.getGroup() != Person.Group.ADMIN) {
			throw new ClientErrorException(FORBIDDEN);
		}

		final String DOCUMENT_QUERY = "UPDATE Person " + "SET position = null WHERE personIdentity = :personIdentity";
		Person updatedRequester = pokerManager.createQuery(DOCUMENT_QUERY, Person.class)
				.setParameter("personIdentity", personToBeRemoved.getIdentity()).getSingleResult();

		try {
			pokerManager.getTransaction().commit();
		} catch (final RollbackException exception) {
			throw new ClientErrorException(CONFLICT);
		} finally {
			pokerManager.getTransaction().begin();
		}

		Cache cache = pokerManager.getEntityManagerFactory().getCache();
		(cache).evict(updatedRequester.getClass(), updatedRequester.getIdentity());

		return position.toString();
	}

	@DELETE
	@Path("{id}/games")
	@Produces(TEXT_PLAIN)
	public String addNewGame(@PathParam("id") @Positive final long identity) {
		final EntityManager pokerManager = RestJpaLifecycleProvider.entityManager("poker");
		final PokerTable pokerTable = pokerManager.find(PokerTable.class, identity);

		if (pokerTable.getPlayers().size() < 2) {
			throw new ClientErrorException(BAD_REQUEST);
		}

		for (Game game : pokerTable.getGames()) {
			if (game.getState() != Game.State.SHOWDOWN) {
				throw new ClientErrorException(BAD_REQUEST);

			}
		}

		Game game = pokerManager.createQuery(
				"INSERT into Game (pokerTableReference, stateAlias) VALUES (:pokerTableReference,:stateAliasstateAlias)",
				Game.class).setParameter("stateAlias", Game.State.SHOWDOWN)
				.setParameter("pokerTableReferenc", pokerTable.getIdentity()).getSingleResult();

		pokerManager.persist(game);

		pokerTable.getGames().add(game);

		try {
			pokerManager.getTransaction().commit();
			pokerManager.flush();
		} catch (final RollbackException exception) {
			throw new ClientErrorException(CONFLICT);
		} finally {
			pokerManager.getTransaction().begin();
		}

		for (Person p : pokerTable.getPlayers()) {
			Hand hand = pokerManager.createQuery(
					"INSERT into Hand (gameReference, playerReference) VALUES (:gameReference,:playerReference)",
					Hand.class).setParameter("stateAlias", Game.State.SHOWDOWN)
					.setParameter("gameReference", game.getIdentity()).setParameter("playerReference", p.getIdentity())
					.getSingleResult();
			pokerManager.persist(hand);
		}

		Hand deck = pokerManager.createQuery("INSERT into Hand (gameReference) VALUES (:gameReference)", Hand.class)
				.setParameter("stateAlias", Game.State.SHOWDOWN).setParameter("gameReference", game.getIdentity())
				.getSingleResult();
		pokerManager.persist(deck);
		
		try {
			pokerManager.getTransaction().commit();
			pokerManager.flush();
		} catch (final RollbackException exception) {
			throw new ClientErrorException(CONFLICT);
		} finally {
			pokerManager.getTransaction().begin();
		}

		// deck

		return Long.toString(game.getIdentity());
	}

}
