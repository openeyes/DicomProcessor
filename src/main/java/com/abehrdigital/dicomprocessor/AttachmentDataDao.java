/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.abehrdigital.dicomprocessor;

import com.abehrdigital.dicomprocessor.models.AttachmentData;
import org.hibernate.Session;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;
import java.util.ArrayList;
import java.util.List;

/**
 * @author admin
 */
public class AttachmentDataDao implements BaseDao<AttachmentData, Integer> {

    private Session session;

    public AttachmentDataDao(Session session) {
        this.session = session;
    }

    @Override
    public AttachmentData get(Integer id) {
        return session.get(AttachmentData.class, id);
    }

    @Override
    public void save(AttachmentData entity) {
        session.save(entity);
    }

    @Override
    public void update(AttachmentData entity) {
        session.update(entity);
    }

    @Override
    public void delete(AttachmentData entity) {
        session.delete(entity);
    }

    public AttachmentData getByAttachmentMnemonicAndBodySite(String attachmentMnemonic, String bodySite, int requestId) {
        CriteriaBuilder criteriaBuilder = session.getCriteriaBuilder();
        CriteriaQuery criteriaQuery = criteriaBuilder.createQuery(AttachmentData.class);
        Root root = criteriaQuery.from(AttachmentData.class);

        List<Predicate> predicates = new ArrayList<>();
        predicates.add(criteriaBuilder.and(criteriaBuilder.equal(root.get("requestId"), requestId)));

        if (attachmentMnemonic != null) {
            predicates.add(criteriaBuilder.and(criteriaBuilder.equal(root.get("attachmentMnemonic"), attachmentMnemonic)));
        }

        if (bodySite != null) {
            predicates.add(criteriaBuilder.and(criteriaBuilder.equal(root.get("bodySiteSnomedType"), bodySite)));
        }


        criteriaQuery.where(predicates.stream().toArray(Predicate[]::new));

        List<AttachmentData> attachmentDataList = session.createQuery(criteriaQuery).getResultList();
        if(attachmentDataList.size() == 0){
            return null;
        } else {
            return attachmentDataList.get(0);
        }
    }
}
