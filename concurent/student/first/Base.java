package concurent.student.first;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
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
    private final List<Building> buildings = Collections.synchronizedList(new LinkedList<>());

    public Base(String name){
        this.name = name;
        for(int i = 0; i < STARTER_PEASANT_NUMBER; i++) {
            peasants.add(createPeasant());
        }
        peasants.get(0).startMining();
        peasants.get(1).startMining();
        peasants.get(2).startMining();
        peasants.get(3).startCuttingWood();
    }

    public void startPreparation(){

        Thread buildingThread = new Thread(() -> {
            while(!hasEnoughBuilding(UnitType.FARM, 3)) {
                if(getFreePeasant() != null) {
                    getFreePeasant().tryBuilding(UnitType.FARM);  
                }
            }
            while(!hasEnoughBuilding(UnitType.LUMBERMILL, 1)) {
                if(getFreePeasant() != null) {
                    getFreePeasant().tryBuilding(UnitType.LUMBERMILL);  
                }
            }
            while(!hasEnoughBuilding(UnitType.BLACKSMITH, 1)) {
                if(getFreePeasant() != null) {
                    getFreePeasant().tryBuilding(UnitType.BLACKSMITH);  
                }
            }
        });
        buildingThread.start();

        Thread peasantThread = new Thread(() -> {
            while(this.peasants.size() != PEASANT_NUMBER_GOAL) {
                Peasant p = createPeasant();
                if(p != null) {
                    peasants.add(p);
                }
            }
            if(getFreePeasant() != null) {
                getFreePeasant().startMining();  
            }
            if(getFreePeasant() != null) {
                getFreePeasant().startMining();  
            }
            if(getFreePeasant() != null) {
                getFreePeasant().startCuttingWood();  
            }
        });
        peasantThread.start();

        try {
            buildingThread.join();
            peasantThread.join();
        } catch (InterruptedException e) {
            System.out.println("Interrupt Occurred");
            e.printStackTrace();
        }
        for(Peasant p : peasants) {
            p.stopHarvesting();
        }
        
        System.out.println(this.name + " finished creating a base");
        System.out.println(this.name + " peasants: " + this.peasants.size());
        for(Building b : buildings){
            System.out.println(this.name + " has a  " + b.getUnitType().toString());
        }

    }


    /**
     * Returns a peasants that is currently free.
     * Being free means that the peasant currently isn't harvesting or building.
     *
     * @return Peasant object, if found one, null if there isn't one
     */
    private Peasant getFreePeasant(){
        for(Peasant p : peasants) {
            if(p.isFree()) {
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
    private Peasant createPeasant(){
        Peasant result;
        if(resources.canTrain(UnitType.PEASANT.goldCost, UnitType.PEASANT.woodCost, UnitType.PEASANT.foodCost)){
            trainingLock.lock();
            try {
                sleepForMsec(UnitType.PEASANT.buildTime);
                getResources().removeCost(UnitType.PEASANT.goldCost, UnitType.PEASANT.woodCost);
                getResources().updateCapacity(UnitType.PEASANT.foodCost);
                result = Peasant.createPeasant(this);
                return result;
            } finally {
                trainingLock.unlock();
            }
        }
        return null;
    }

    public Resources getResources(){
        return this.resources;
    }

    public List<Building> getBuildings(){
        return this.buildings;
    }

    public String getName(){
        return this.name;
    }

    /**
     * Helper method to determine if a base has the required number of a certain building.
     *
     * @param unitType Type of the building
     * @param required Number of required amount
     * @return true, if required amount is reached (or surpassed), false otherwise
     */
    private boolean hasEnoughBuilding(UnitType unitType, int required){
        int count = 0;
        for(Building building : getBuildings()) {
            if(building.getUnitType() == unitType) {
                count++;
            }
            if(count >= required) {
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
