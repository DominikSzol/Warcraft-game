package concurent.student.second;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;

public class Base {

    private static final int STARTER_PEASANT_NUMBER = 5;
    private static final int PEASANT_NUMBER_GOAL = 10;

    // lock to ensure only one unit can be trained at one time
    private final ReentrantLock trainingLock = new ReentrantLock();

    private final String name;
    private final Resources resources = new Resources();
    private final List<Peasant> peasants = Collections.synchronizedList(new LinkedList<>());
    private final List<Footman> footmen = Collections.synchronizedList(new LinkedList<>());
    private final List<Building> buildings = Collections.synchronizedList(new LinkedList<>());
    private final List<Personnel> army = Collections.synchronizedList(new LinkedList<>());

    public Base(String name) {
        this.name = name;
        for (int i = 0; i < STARTER_PEASANT_NUMBER; i++) {
            peasants.add(createPeasant());
        }
        peasants.get(0).startMining();
        peasants.get(1).startMining();
        peasants.get(2).startMining();
        peasants.get(3).startCuttingWood();
    }

    public void startPreparation() {
        Thread buildingThread = new Thread(() -> {
            while (!hasEnoughBuilding(UnitType.FARM, 3)) {
                if (getFreePeasant() != null) {
                    getFreePeasant().tryBuilding(UnitType.FARM);
                }
            }
            while (!hasEnoughBuilding(UnitType.LUMBERMILL, 1)) {
                if (getFreePeasant() != null) {
                    getFreePeasant().tryBuilding(UnitType.LUMBERMILL);
                }
            }
            while (!hasEnoughBuilding(UnitType.BLACKSMITH, 1)) {
                if (getFreePeasant() != null) {
                    getFreePeasant().tryBuilding(UnitType.BLACKSMITH);
                }
            }
            while (!hasEnoughBuilding(UnitType.BARRACKS, 1)) {
                if (getFreePeasant() != null) {
                    getFreePeasant().tryBuilding(UnitType.BARRACKS);
                }
            }
        });
        buildingThread.start();

        Thread peasantThread = new Thread(() -> {
            while (this.peasants.size() != PEASANT_NUMBER_GOAL) {
                Peasant p = createPeasant();
                if (p != null) {
                    peasants.add(p);
                }
            }
            if (getFreePeasant() != null) {
                getFreePeasant().startMining();
            }
            if (getFreePeasant() != null) {
                getFreePeasant().startMining();
            }
            if (getFreePeasant() != null) {
                getFreePeasant().startCuttingWood();
            }
        });
        peasantThread.start();

        Thread footmanThread = new Thread(() -> {
            while (this.footmen.size() != 10) {
                Footman f = this.createFootman();
                if (f != null) {
                    this.footmen.add(f);
                }
            }
        });
        footmanThread.start();

        try {
            buildingThread.join();
            peasantThread.join();
            footmanThread.join();
        } catch (InterruptedException e) {
            System.out.println("Interrupt Occurred");
            e.printStackTrace();
        }
        for (Peasant p : peasants) {
            p.stopHarvesting();
        }
        System.out.println(this.name + " finished creating a base");
        System.out.println(this.name + " peasants: " + this.peasants.size());
        System.out.println(this.name + " footmen: " + this.footmen.size());
        for (Building b : buildings) {
            System.out.println(this.name + " has a  " + b.getUnitType().toString());
        }
    }

    /**
     * Assemble the army - call the peasants and footmen to arms
     * 
     * @param latch
     */
    public void assembleArmy(CountDownLatch latch) {
        for (Peasant p : this.peasants) {
            this.army.add(p);
        }
        for (Footman f : this.footmen) {
            this.army.add(f);
        }
        System.out.println(this.name + " is ready for war");
        // the latch is used to keep track of both factions
        latch.countDown();
    }

    /**
     * Starts a war between the two bases.
     *
     * @param enemy    Enemy base's personnel
     * @param warLatch Latch to make sure they attack at the same time
     */
    public void goToWar(List<Personnel> enemy, CountDownLatch warLatch) {
        // This is necessary to ensure that both armies attack at the same time
        warLatch.countDown();
        try {
            // Waiting for the other army to be ready for war
            warLatch.await();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        Thread fight = new Thread(() -> {
            for (int i = 0; i < army.size(); i++) {
                army.get(i).startWar(enemy);
            }
            while(!army.isEmpty() && !enemy.isEmpty()) {
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        });
        fight.start();
        try {
            fight.join();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        // If our army has no personnel, we failed
        if (army.isEmpty()) {
            System.out.println(this.name + " has lost the fight");
        } else {
            System.out.println(this.name + " has won the fight");
        }
    }

    /**
     * Resolves the event when a personnel dies;
     * Remove it from the army and update the capacity.
     * 
     * @param p The fallen personnel
     */
    public void signalPersonnelDeath(Personnel p) {
        this.getResources().updateCapacity(-(p.getUnitType().foodCost));
        this.army.remove(p);
        System.out.println(this.name + " has lost a " + p.getUnitType().toString());

    }

    /**
     * Returns a peasants that is currently free.
     * Being free means that the peasant currently isn't harvesting or building.
     *
     * @return Peasant object, if found one, null if there isn't one
     */
    private Peasant getFreePeasant() {
        for (Peasant p : peasants) {
            if (p.isFree()) {
                return p;
            }
        }
        return null;
    }

    /**
     * Creates a peasant.
     * A peasant could only be trained if there are sufficient
     * gold, wood and food for him to train.
     *
     * At one time only one Peasant can be trained.
     *
     * @return The newly created peasant if it could be trained, null otherwise
     */
    private Peasant createPeasant() {
        Peasant result;
        if (resources.canTrain(UnitType.PEASANT.goldCost, UnitType.PEASANT.woodCost, UnitType.PEASANT.foodCost)) {
            this.trainingLock.lock();
            try {
                sleepForMsec(UnitType.PEASANT.buildTime);
                getResources().removeCost(UnitType.PEASANT.goldCost, UnitType.PEASANT.woodCost);
                getResources().updateCapacity(UnitType.PEASANT.foodCost);
                result = Peasant.createPeasant(this);
                return result;
            } finally {
                this.trainingLock.unlock();
            }
        }
        return null;
    }

    private Footman createFootman() {
        Footman result;
        if (resources.canTrain(UnitType.FOOTMAN.goldCost, UnitType.FOOTMAN.woodCost, UnitType.FOOTMAN.foodCost)
                && hasEnoughBuilding(UnitType.BARRACKS, 1)) {
            this.trainingLock.lock();
            try {
                sleepForMsec(UnitType.FOOTMAN.buildTime);
                getResources().removeCost(UnitType.FOOTMAN.goldCost, UnitType.FOOTMAN.woodCost);
                getResources().updateCapacity(UnitType.FOOTMAN.foodCost);
                result = Footman.createFootman(this);
            } finally {
                this.trainingLock.unlock();
            }
            System.out.println(this.name + " created a footman");
            return result;
        }
        return null;
    }

    public Resources getResources() {
        return this.resources;
    }

    public List<Personnel> getArmy() {
        return this.army;
    }

    public List<Building> getBuildings() {
        return this.buildings;
    }

    public String getName() {
        return this.name;
    }

    /**
     * Helper method to determine if a base has the required number of a certain
     * building.
     *
     * @param unitType Type of the building
     * @param required Number of required amount
     * @return true, if required amount is reached (or surpassed), false otherwise
     */
    private boolean hasEnoughBuilding(UnitType unitType, int required) {
        int count = 0;
        for (Building building : this.getBuildings()) {
            if (building.getUnitType() == unitType) {
                count++;
            }
            if (count >= required) {
                return true;
            }
        }
        return false;
    }

    private static void sleepForMsec(int sleepTime) {
        try {
            TimeUnit.MILLISECONDS.sleep(sleepTime);
        } catch (InterruptedException e) {
        }
    }

}
