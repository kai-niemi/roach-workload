package io.roach.workload.orders.model;

import java.io.Serializable;
import java.time.LocalDate;
import java.util.UUID;

import javax.persistence.*;

import org.hibernate.annotations.Columns;
import org.springframework.util.Assert;

import io.roach.workload.common.jpa.AbstractEntity;
import io.roach.workload.common.util.Money;

@Entity
@Inheritance(strategy = InheritanceType.TABLE_PER_CLASS)
public abstract class AbstractOrder extends AbstractEntity<AbstractOrder.Id> {
    @EmbeddedId
    private AbstractOrder.Id id;

    @Embeddable
    public static class Id implements Serializable, Comparable<Id> {
        @Column(name = "id", updatable = false)
        private UUID uuid;

        @Column(nullable = false, name = "date_placed", updatable = false)
        @Basic(fetch = FetchType.LAZY)
        private LocalDate datePlaced;

        protected Id() {
        }

        public Id(UUID uuid, LocalDate datePlaced) {
            Assert.notNull(uuid, "uuid is required");
            Assert.notNull(datePlaced, "datePlaced is required");
            this.uuid = uuid;
            this.datePlaced = datePlaced;
        }

        public static Id of(UUID accountId, LocalDate datePlaced) {
            return new Id(accountId, datePlaced);
        }

        public static Id of(UUID accountId) {
            return new Id(accountId, null);
        }

        public UUID getUUID() {
            return uuid;
        }

        public LocalDate getDatePlaced() {
            return datePlaced;
        }

        @Override
        public int compareTo(Id o) {
            return o.uuid.compareTo(uuid);
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }

            Id id = (Id) o;

            if (!uuid.equals(id.uuid)) {
                return false;
            }
            return datePlaced.equals(id.datePlaced);
        }

        @Override
        public int hashCode() {
            int result = uuid.hashCode();
            result = 31 * result + datePlaced.hashCode();
            return result;
        }

        @Override
        public String toString() {
            return "Id{" +
                    "uuid=" + uuid +
                    ", datePlaced=" + datePlaced +
                    '}';
        }
    }

    @Column(name = "order_number")
    private Integer orderNumber;

    @Column(nullable = false, name = "date_updated")
    @Basic(fetch = FetchType.LAZY)
    private LocalDate dateUpdated;

    @Column(name = "bill_to_first_name")
    private String billToFirstName;

    @Column(name = "bill_to_last_name")
    private String billToLastName;

    @Embedded
    @AttributeOverrides({
            @AttributeOverride(name = "address1",
                    column = @Column(name = "bill_address1", length = 255)),
            @AttributeOverride(name = "address2",
                    column = @Column(name = "bill_address2", length = 255)),
            @AttributeOverride(name = "city",
                    column = @Column(name = "bill_city", length = 255)),
            @AttributeOverride(name = "postcode",
                    column = @Column(name = "bill_postcode", length = 16)),
            @AttributeOverride(name = "country.code",
                    column = @Column(name = "bill_country_code", length = 16)),
            @AttributeOverride(name = "country.name",
                    column = @Column(name = "bill_country_name", length = 16))
    })
    private Address billAddress;

    @Column(name = "deliv_to_first_name")
    private String deliverToFirstName;

    @Column(name = "deliv_to_last_name")
    private String deliverToLastName;

    @Embedded
    @AttributeOverrides({
            @AttributeOverride(name = "address1",
                    column = @Column(name = "deliv_address1", length = 255)),
            @AttributeOverride(name = "address2",
                    column = @Column(name = "deliv_address2", length = 255)),
            @AttributeOverride(name = "city",
                    column = @Column(name = "deliv_city", length = 255)),
            @AttributeOverride(name = "postcode",
                    column = @Column(name = "deliv_postcode", length = 16)),
            @AttributeOverride(name = "country.code",
                    column = @Column(name = "deliv_country_code", length = 16)),
            @AttributeOverride(name = "country.name",
                    column = @Column(name = "deliv_country_name", length = 16))
    })
    private Address deliveryAddress;

    @Columns(columns = {
            @Column(name = "total_price_amount", nullable = false, updatable = false),
            @Column(name = "total_price_currency", nullable = false, updatable = false, length = 3)
    })
    private Money totalPrice;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", length = 64)
    private ShipmentStatus status = ShipmentStatus.PLACED;

    @Column(name = "customer_id")
    private UUID customerId;

    @Column(name = "payment_method_id")
    private UUID paymentMethod;

    @Transient
    private Customer customer;

    public AbstractOrder() {

    }

    public Customer getCustomer() {
        return customer;
    }

    public AbstractOrder setCustomer(Customer customer) {
        this.customer = customer;
        return this;
    }

    public AbstractOrder setId(Id id) {
        this.id = id;
        return this;
    }

    @Override
    public Id getId() {
        return id;
    }

    public Integer getOrderNumber() {
        return orderNumber;
    }

    public void setOrderNumber(Integer orderNumber) {
        this.orderNumber = orderNumber;
    }

    public LocalDate getDatePlaced() {
        return id.getDatePlaced();
    }

    public String getBillToFirstName() {
        return billToFirstName;
    }

    public String getBillToLastName() {
        return billToLastName;
    }

    public Address getBillAddress() {
        return billAddress;
    }

    public String getDeliverToFirstName() {
        return deliverToFirstName;
    }

    public String getDeliverToLastName() {
        return deliverToLastName;
    }

    public Address getDeliveryAddress() {
        return deliveryAddress;
    }

    public Money getTotalPrice() {
        return totalPrice;
    }

    public ShipmentStatus getStatus() {
        return status;
    }

    public UUID getCustomerId() {
        return customerId;
    }

    public UUID getPaymentMethod() {
        return paymentMethod;
    }

    public LocalDate getDateUpdated() {
        return dateUpdated;
    }

    public AbstractOrder setDateUpdated(LocalDate dateUpdated) {
        this.dateUpdated = dateUpdated;
        return this;
    }

    public void setBillToFirstName(String billToFirstName) {
        this.billToFirstName = billToFirstName;
    }

    public void setBillToLastName(String billToLastName) {
        this.billToLastName = billToLastName;
    }

    public void setBillAddress(Address billAddress) {
        this.billAddress = billAddress;
    }

    public void setDeliverToFirstName(String deliverToFirstName) {
        this.deliverToFirstName = deliverToFirstName;
    }

    public void setDeliverToLastName(String deliverToLastName) {
        this.deliverToLastName = deliverToLastName;
    }

    public void setDeliveryAddress(Address deliveryAddress) {
        this.deliveryAddress = deliveryAddress;
    }

    public void setTotalPrice(Money totalPrice) {
        this.totalPrice = totalPrice;
    }

    public void setStatus(ShipmentStatus status) {
        this.status = status;
    }

    public void setCustomerId(UUID customerId) {
        this.customerId = customerId;
    }

    public void setPaymentMethod(UUID paymentMethod) {
        this.paymentMethod = paymentMethod;
    }

    @Override
    public String toString() {
        return "AbstractOrder{" +
                "id=" + id +
                ", orderNumber=" + orderNumber +
                ", dateUpdated=" + dateUpdated +
                ", billToFirstName='" + billToFirstName + '\'' +
                ", billToLastName='" + billToLastName + '\'' +
                ", billAddress=" + billAddress +
                ", deliverToFirstName='" + deliverToFirstName + '\'' +
                ", deliverToLastName='" + deliverToLastName + '\'' +
                ", deliveryAddress=" + deliveryAddress +
                ", totalPrice=" + totalPrice +
                ", status=" + status +
                ", customerId=" + customerId +
                ", paymentMethod=" + paymentMethod +
                ", customer=" + customer +
                "} " + super.toString();
    }
}

