package com.abehrdigital.payloadprocessor.dao;

import com.abehrdigital.payloadprocessor.models.RequestRoutine;
import com.abehrdigital.payloadprocessor.utils.QueryResultUtils;
import com.abehrdigital.payloadprocessor.utils.Status;
import org.hibernate.Session;
import org.hibernate.query.NativeQuery;

import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Root;
import java.util.List;


public class RequestRoutineDao implements BaseDao<RequestRoutine, Integer> {
    private Session session;
    private final static int EXECUTE_SEQUENCE_NEW_ROUTINE_INCREMENT = 10;

    public RequestRoutineDao(Session session) {
        this.session = session;
    }

    @Override
    public RequestRoutine get(Integer id) {
        return session.get(RequestRoutine.class, id);
    }

    @Override
    public void save(RequestRoutine entity) {
        session.save(entity);
    }

    public void saveWithNewExecutionSequence(RequestRoutine entity, boolean isPriority) {
        int executeSequence;
        if(isPriority) {
            executeSequence = 1;
        } else {
            executeSequence = getNextExecutionSequence(entity.getRequestId());
        }
        entity.setExecuteSequence(executeSequence);
        save(entity);
    }

    public void saveWithNewExecutionSequence(RequestRoutine entity) {
        saveWithNewExecutionSequence(entity, false);
    }

    private int getNextExecutionSequence(int requestId) {
        CriteriaBuilder criteriaBuilder = session.getCriteriaBuilder();
        CriteriaQuery<RequestRoutine> criteriaQuery = criteriaBuilder.createQuery(RequestRoutine.class);
        Root<RequestRoutine> root = criteriaQuery.from(RequestRoutine.class);
        criteriaQuery.where(criteriaBuilder.equal(root.get("requestId"), requestId));
        criteriaQuery.orderBy(criteriaBuilder.desc(root.get("id")));
        RequestRoutine latestRoutine = session.createQuery(criteriaQuery).setMaxResults(1).getSingleResult();
        return (latestRoutine.getExecuteSequence() + EXECUTE_SEQUENCE_NEW_ROUTINE_INCREMENT);
    }

    @Override
    public void update(RequestRoutine entity) {
        session.update(entity);
    }

    @Override
    public void delete(RequestRoutine entity) {
        session.delete(entity);
    }

    @SuppressWarnings("unchecked")
    public synchronized List<RequestRoutine> getRoutinesForQueueProcessing(String requestQueue) {
        NativeQuery query = session.getNamedNativeQuery("routinesWithRequestQueueRestrictionForProcessing");
        synchronized (query) {
            query.addEntity("rr", RequestRoutine.class);
            query.setParameter("request_queue", requestQueue);
            query.setParameter("new_status", Status.NEW.toString());
            query.setParameter("retry_status", Status.RETRY.toString());
            query.setParameter("complete_status", Status.COMPLETE.toString());
            query.setParameter("void_status", Status.VOID.toString());

            return query.getResultList();
        }
    }

    public RequestRoutine getRequestRoutineWithRequestIdForProcessing(int requestId, String requestQueue) {
        NativeQuery query = session.getNamedNativeQuery("routinesWithRequestQueueAndRequestIdRestrictionForProcessing");
        query.addEntity("rr", RequestRoutine.class);
        query.setParameter("request_queue", requestQueue);
        query.setParameter("request_id", requestId);
        query.setParameter("new_status", Status.NEW.toString());
        query.setParameter("retry_status", Status.RETRY.toString());
        query.setParameter("complete_status", Status.COMPLETE.toString());
        query.setParameter("void_status", Status.VOID.toString());

        return (RequestRoutine) QueryResultUtils.getFirstResultOrNull(query);
    }

    public RequestRoutine findByRoutineNameAndRequestId(int requestId, String routineName) {
        CriteriaBuilder criteriaBuilder = session.getCriteriaBuilder();
        CriteriaQuery<RequestRoutine> criteriaQuery = criteriaBuilder.createQuery(RequestRoutine.class);
        Root<RequestRoutine> root = criteriaQuery.from(RequestRoutine.class);

        criteriaQuery.where(
                criteriaBuilder.and(
                        criteriaBuilder.equal(root.get("requestId"), requestId),
                        criteriaBuilder.equal(root.get("routineName"), routineName)
                )
        );

        return (RequestRoutine) QueryResultUtils.getFirstResultOrNull(session.createQuery(criteriaQuery));
    }

    public void resetAndSave(RequestRoutine requestRoutine) {
        requestRoutine.reset();
        update(requestRoutine);
    }

    public List<RequestRoutine> findNotCompletedByRequestId(int requestId) {
        CriteriaBuilder criteriaBuilder = session.getCriteriaBuilder();
        CriteriaQuery<RequestRoutine> criteriaQuery = criteriaBuilder.createQuery(RequestRoutine.class);
        Root<RequestRoutine> root = criteriaQuery.from(RequestRoutine.class);

        CriteriaBuilder.In<Status> inClause = criteriaBuilder.in(root.get("status"));
        inClause.value(Status.NEW);
        inClause.value(Status.RETRY);

        criteriaQuery.where(
                criteriaBuilder.and(
                        criteriaBuilder.equal(root.get("requestId"), requestId)
                ),
                inClause
        );

        return session.createQuery(criteriaQuery).getResultList();

    }
}
