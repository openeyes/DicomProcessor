package com.abehrdigital.payloadprocessor.dao;

import com.abehrdigital.payloadprocessor.models.AttachmentData;
import com.abehrdigital.payloadprocessor.models.RequestDetails;
import com.abehrdigital.payloadprocessor.models.RoutineLibrary;
import com.abehrdigital.payloadprocessor.utils.QueryResultUtils;
import org.hibernate.Session;

import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;
import java.util.ArrayList;
import java.util.List;


public class RequestDetailsDao implements BaseDao<RequestDetails, String> {
    private Session session;

    public RequestDetailsDao(Session session) {
        this.session = session;
    }

    @Override
    public RequestDetails get(String routineName) {
        return session.get(RequestDetails.class, routineName);
    }

    @Override
    public void save(RequestDetails entity) {
        session.save(entity);
    }

    @Override
    public void update(RequestDetails entity) {
        session.update(entity);
    }

    @Override
    public void delete(RequestDetails entity) {
        session.delete(entity);
    }

    public RequestDetails getByName(String name, int requestId) {
        CriteriaBuilder criteriaBuilder = session.getCriteriaBuilder();
        CriteriaQuery<RequestDetails> criteriaQuery = criteriaBuilder.createQuery(RequestDetails.class);
        Root root = criteriaQuery.from(RequestDetails.class);

        List<Predicate> predicates = new ArrayList<>();
        predicates.add(criteriaBuilder.and(criteriaBuilder.equal(root.get("requestId"), requestId)));
        predicates.add(criteriaBuilder.and(criteriaBuilder.equal(root.get("name"), name)));

        criteriaQuery.where(predicates.toArray(new Predicate[0]));

        return (RequestDetails) QueryResultUtils.getFirstResultOrNull(session.createQuery(criteriaQuery));
    }
}

