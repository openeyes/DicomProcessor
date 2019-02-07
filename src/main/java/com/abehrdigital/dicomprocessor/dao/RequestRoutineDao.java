package com.abehrdigital.dicomprocessor.dao;

import com.abehrdigital.dicomprocessor.models.RequestRoutine;
import com.abehrdigital.dicomprocessor.utils.Status;
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
        int executeSequence = getNextExecutionSequence();
        entity.setExecuteSequence(executeSequence);
        session.save(entity);
    }

    private int getNextExecutionSequence() {
        CriteriaBuilder criteriaBuilder = session.getCriteriaBuilder();
        CriteriaQuery<RequestRoutine> criteriaQuery = criteriaBuilder.createQuery(RequestRoutine.class);
        Root<RequestRoutine> root = criteriaQuery.from(RequestRoutine.class);
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

    public List<RequestRoutine> getRoutinesForQueueProcessing(String requestQueue) {
        NativeQuery query = session.createSQLQuery("" +
                "SELECT * FROM request_routine rr " +
                "WHERE rr.execute_request_queue = :request_queue " +
                "AND rr.status IN( :new_status , :retry_status) " +
                "AND IFNULL (rr.next_try_date_time, SYSDATE()) <= SYSDATE() " +
                "AND NOT EXISTS ( " +
                "SELECT * " +
                "FROM request_routine subrr " +
                "WHERE subrr.request_id = rr.request_id " +
                "AND subrr.id < rr.id " +
                "AND subrr.status NOT IN (:complete_status , :void_status) " +
                ")" +
                "ORDER BY rr.execute_sequence"
        )
                .addEntity("rr", RequestRoutine.class)
                .setParameter("request_queue", requestQueue)
                .setParameter("new_status", Status.NEW.toString())
                .setParameter("retry_status", Status.RETRY.toString())
                .setParameter("complete_status", Status.COMPLETE.toString())
                .setParameter("void_status", Status.VOID.toString());

        return query.getResultList();
    }

    public RequestRoutine getRequestRoutineWithRequestIdForProcessing(int requestId, String requestQueue) {
        NativeQuery query = session.createSQLQuery("" +
                "SELECT * FROM request_routine rr " +
                "WHERE rr.execute_request_queue = :request_queue " +
                "AND rr.status IN( :new_status , :retry_status) " +
                "AND rr.request_id = :request_id " +
                "AND IFNULL (rr.next_try_date_time, SYSDATE()) <= SYSDATE() " +
                "AND NOT EXISTS ( " +
                "SELECT * " +
                "FROM request_routine subrr " +
                "WHERE subrr.request_id = rr.request_id " +
                "AND subrr.id < rr.id " +
                "AND subrr.status NOT IN (:complete_status , :void_status) " +
                ")" +
                "ORDER BY rr.execute_sequence"
        )
                .addEntity("rr", RequestRoutine.class)
                .setParameter("request_queue", requestQueue)
                .setParameter("request_id", requestId)
                .setParameter("new_status", Status.NEW.toString())
                .setParameter("retry_status", Status.RETRY.toString())
                .setParameter("complete_status", Status.COMPLETE.toString())
                .setParameter("void_status", Status.VOID.toString());

        List<RequestRoutine> requestRoutines = query.getResultList();

        if (requestRoutines.size() == 0) {
            return null;
        } else {
            return requestRoutines.get(0);
        }
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

        List<RequestRoutine> requestRoutines = session.createQuery(criteriaQuery).getResultList();

        if (requestRoutines.size() == 0) {
            return null;
        } else {
            return session.createQuery(criteriaQuery).getResultList().get(0);
        }
    }

    public void resetRequestRoutine(RequestRoutine requestRoutine) {
        requestRoutine.reset();
        update(requestRoutine);
    }
}
