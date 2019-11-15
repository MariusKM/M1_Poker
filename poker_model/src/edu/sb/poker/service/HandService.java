package edu.sb.poker.service;

import static edu.sb.poker.persistence.Person.Group.ADMIN;
import static edu.sb.poker.service.BasicAuthenticationFilter.REQUESTER_IDENTITY;
import static javax.ws.rs.core.MediaType.APPLICATION_JSON;
import static javax.ws.rs.core.MediaType.APPLICATION_XML;
import static javax.ws.rs.core.MediaType.TEXT_PLAIN;
import static javax.ws.rs.core.MediaType.WILDCARD;
import static javax.ws.rs.core.Response.Status.CONFLICT;
import static javax.ws.rs.core.Response.Status.FORBIDDEN;
import static javax.ws.rs.core.Response.Status.NOT_ACCEPTABLE;
import static javax.ws.rs.core.Response.Status.NOT_FOUND;
import static javax.ws.rs.core.Response.Status.BAD_REQUEST;

import java.io.IOException;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

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
import javax.ws.rs.core.Response;

import org.eclipse.persistence.oxm.MediaType;
import edu.sb.poker.persistence.BaseEntity;
import edu.sb.poker.persistence.Card;
import edu.sb.poker.persistence.Document;
import edu.sb.poker.persistence.Game;
import edu.sb.poker.persistence.Hand;
import edu.sb.poker.persistence.Person;
import edu.sb.poker.persistence.PokerTable;
import edu.sb.poker.util.HashCodes;
import edu.sb.poker.util.RestJpaLifecycleProvider;

@Path("hands")
public class HandService {

	@GET
	@Path("{id}")
	public Hand getHand(@PathParam("id") @Positive final long identity) {
		final EntityManager messengerManager = RestJpaLifecycleProvider.entityManager("poker");

		Hand hand = messengerManager.find(Hand.class, identity);
		if (hand == null) {
			throw new ClientErrorException(NOT_FOUND);
		}

		return hand;
	}

	@GET
	@Path("{id}/cards")
	public Set<Card> getCards(@HeaderParam(REQUESTER_IDENTITY) @Positive final long requesterIdentity,
			@PathParam("id") @Positive final long identity) {
		final EntityManager pokerManager = RestJpaLifecycleProvider.entityManager("poker");
		Person requester = pokerManager.find(Person.class, requesterIdentity);
		Hand hand = pokerManager.createQuery("SELECT * FROM Hand WHERE playerReference = :identity", Hand.class)
				.getSingleResult();
		Person handOwner = pokerManager.find(Person.class, hand.getPlayer().getIdentity());
		Game game = pokerManager.find(Game.class, hand.getGame().getIdentity());

		if (requester.getGroup() == Person.Group.ADMIN || requester.getIdentity() == hand.getPlayer().getIdentity()) {

			return hand.getCards();
		} else if (handOwner != null && game.getState() == Game.State.SHOWDOWN && !hand.getFolded()) {
			for (Hand h : game.getHands()) {
				if (!h.getFolded()) {
					return hand.getCards();
				}
			}

		} else {
			throw new ClientErrorException(FORBIDDEN);
		}
		return null;
	}

}
