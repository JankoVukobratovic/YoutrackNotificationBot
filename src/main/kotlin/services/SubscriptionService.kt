package services

import repository.Repository

object SubscriptionService {

    public fun getAllSubscriptions(): Set<Long>{
        return Repository.saveData.subscribedIds;
    }

    /**
     * Adds an element if and only if it was not already subscribed,
     * in which case it saves the list
     * @return Returns true if a new element was added
     */
    public fun add(newId: Long): Boolean {
        return Repository.addSubscription(newId)
    }

    /**
     * Removes an element if and only if it was in the subscription list,
     * in which case it saves the list
     * @return Returns true if an element was actually removed
     */
    fun remove(id: Long): Boolean {
        return Repository.removeSubscription(id)
    }
}