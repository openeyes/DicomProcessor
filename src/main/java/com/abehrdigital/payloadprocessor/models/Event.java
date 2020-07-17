package com.abehrdigital.payloadprocessor.models;

import java.io.Serializable;
import java.util.Date;
import javax.persistence.Basic;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.Table;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;
import javax.xml.bind.annotation.XmlRootElement;

@Entity
@Table(name = "event")
@XmlRootElement
@NamedQueries({
        @NamedQuery(name = "Event.findAll", query = "SELECT e FROM Event e"),
        @NamedQuery(name = "Event.findById", query = "SELECT e FROM Event e WHERE e.id = :id"),
        @NamedQuery(name = "Event.findByLastModifiedDate", query = "SELECT e FROM Event e WHERE e.lastModifiedDate = :lastModifiedDate"),
        @NamedQuery(name = "Event.findByCreatedDate", query = "SELECT e FROM Event e WHERE e.createdDate = :createdDate"),
        @NamedQuery(name = "Event.findByEventDate", query = "SELECT e FROM Event e WHERE e.eventDate = :eventDate"),
        @NamedQuery(name = "Event.findByInfo", query = "SELECT e FROM Event e WHERE e.info = :info"),
        @NamedQuery(name = "Event.findByDeleted", query = "SELECT e FROM Event e WHERE e.deleted = :deleted"),
        @NamedQuery(name = "Event.findByDeleteReason", query = "SELECT e FROM Event e WHERE e.deleteReason = :deleteReason"),
        @NamedQuery(name = "Event.findByDeletePending", query = "SELECT e FROM Event e WHERE e.deletePending = :deletePending")})
public class Event implements Serializable {
    private static final long serialVersionUID = 1L;
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Basic(optional = false)
    @Column(name = "id")
    private Integer id;
    @Basic(optional = false)
    @Column(name = "last_modified_date")
    @Temporal(TemporalType.TIMESTAMP)
    private Date lastModifiedDate;
    @Basic(optional = false)
    @Column(name = "created_date")
    @Temporal(TemporalType.TIMESTAMP)
    private Date createdDate;
    @Basic(optional = false)
    @Column(name = "event_date")
    @Temporal(TemporalType.TIMESTAMP)
    private Date eventDate;
    @Column(name = "info")
    private String info;
    @Basic(optional = false)
    @Column(name = "deleted")
    private boolean deleted;
    @Column(name = "delete_reason")
    private String deleteReason;
    @Basic(optional = false)
    @Column(name = "delete_pending")
    private boolean deletePending;
    @Basic(optional = true)
    @Column(name = "episode_id")
    private Integer episodeId;
    @JoinColumn(name = "event_type_id", referencedColumnName = "id")
    @ManyToOne(optional = false)
    private EventType eventTypeId;
    @Basic(optional = false)
    @Column(name = "created_user_id")
    private Integer createdUserId;
    @Basic(optional = false)
    @Column(name = "last_modified_user_id")
    private Integer lastModifiedUserId;

    public Event() {
    }

    public Event(Integer id) {
        this.id = id;
    }

    public Event(Integer id, Date lastModifiedDate, Date createdDate, Date eventDate, boolean deleted, boolean deletePending) {
        this.id = id;
        this.lastModifiedDate = lastModifiedDate;
        this.createdDate = createdDate;
        this.eventDate = eventDate;
        this.deleted = deleted;
        this.deletePending = deletePending;
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public Date getLastModifiedDate() {
        return lastModifiedDate;
    }

    public void setLastModifiedDate(Date lastModifiedDate) {
        this.lastModifiedDate = lastModifiedDate;
    }

    public Date getCreatedDate() {
        return createdDate;
    }

    public void setCreatedDate(Date createdDate) {
        this.createdDate = createdDate;
    }

    public Date getEventDate() {
        return eventDate;
    }

    public void setEventDate(Date eventDate) {
        this.eventDate = eventDate;
    }

    public String getInfo() {
        return info;
    }

    public void setInfo(String info) {
        this.info = info;
    }

    public boolean getDeleted() {
        return deleted;
    }

    public void setDeleted(boolean deleted) {
        this.deleted = deleted;
    }

    public String getDeleteReason() {
        return deleteReason;
    }

    public void setDeleteReason(String deleteReason) {
        this.deleteReason = deleteReason;
    }

    public boolean getDeletePending() {
        return deletePending;
    }

    public void setDeletePending(boolean deletePending) {
        this.deletePending = deletePending;
    }


    public Integer getEpisodeId() {
        return episodeId;
    }

    public void setEpisodeId(Integer episodeId) {
        this.episodeId = episodeId;
    }

    public EventType getEventTypeId() {
        return eventTypeId;
    }

    public void setEventTypeId(EventType eventTypeId) {
        this.eventTypeId = eventTypeId;
    }

    public Integer getCreatedUserId() {
        return createdUserId;
    }

    public void setCreatedUserId(Integer createdUserId) {
        this.createdUserId = createdUserId;
    }

    public Integer getLastModifiedUserId() {
        return lastModifiedUserId;
    }

    public void setLastModifiedUserId(Integer lastModifiedUserId) {
        this.lastModifiedUserId = lastModifiedUserId;
    }



    @Override
    public int hashCode() {
        int hash = 0;
        hash += (id != null ? id.hashCode() : 0);
        return hash;
    }

    @Override
    public boolean equals(Object object) {
        // TODO: Warning - this method won't work in the case the id fields are not set
        if (!(object instanceof Event)) {
            return false;
        }
        Event other = (Event) object;
        if ((this.id == null && other.id != null) || (this.id != null && !this.id.equals(other.id))) {
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        return "uk.org.openeyes.models.Event[ id=" + id + " ]";
    }

}