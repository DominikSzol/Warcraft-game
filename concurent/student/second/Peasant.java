package concurent.student.second;

import java.util.concurrent.atomic.AtomicBoolean;

public class Peasant extends Personnel {

    private static final int HARVEST_WAIT_TIME = 100;
    private static final int HARVEST_AMOUNT = 10;

    private AtomicBoolean isHarvesting = new AtomicBoolean(false);
    private AtomicBoolean isBuilding = new AtomicBoolean(false);

    private Peasant(Base owner) {
        super(220, owner, 5, 6, UnitType.PEASANT);
    }

    public static Peasant createPeasant(Base owner){
        return new Peasant(owner);
    }

    /**
     * Starts gathering gold.
     */
    public void startMining(){
        this.isHarvesting.set(true);
        new Thread(() -> {
            try {
                while(isHarvesting.get()) {
                    Thread.sleep(HARVEST_WAIT_TIME);
                    this.getOwner().getResources().addGold(HARVEST_AMOUNT);
                }
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }).start();
        System.out.println("Peasant starting mining");
    }

    /**
     * Starts gathering wood.
     */
    public void startCuttingWood(){
        this.isHarvesting.set(true);
        new Thread(() -> {
            try {
                while(isHarvesting.get()) {
                    Thread.sleep(HARVEST_WAIT_TIME);
                    this.getOwner().getResources().addWood(HARVEST_AMOUNT);
                }
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }).start();
        System.out.println("Peasant starting cutting wood");
    }

    /**
     * Peasant should stop all harvesting once this is invoked
     */
    public void stopHarvesting(){
        this.isHarvesting.set(false);
    }

    /**
     * Tries to build a certain type of building.
     * Can only build if there are enough gold and wood for the building
     * to be built.
     *
     * @param buildingType Type of the building
     * @return true, if the building process has started
     *         false, if there are insufficient resources
     */
    public boolean tryBuilding(UnitType buildingType){
        if(this.getOwner().getResources().canBuild(buildingType.goldCost,buildingType.woodCost)) {
            Thread thread = new Thread(() -> {
                startBuilding(buildingType);
            });
            thread.start();
            try {
                thread.join();
            } catch (InterruptedException e) {
                System.out.println("Interrupt Occurred");
                e.printStackTrace();
            }
            return true;
        }
        return false;
    }

    /**
     * Start building a certain type of building.
     * Keep in mind that a peasant can only build one building at one time.
     *
     * @param buildingType Type of the building
     */
    private void startBuilding(UnitType buildingType){
        if(!this.isBuilding.get()) {
            this.isBuilding.set(true);
            this.getOwner().getResources().removeCost(buildingType.goldCost, buildingType.woodCost);
            sleepForMsec(buildingType.buildTime);
            this.getOwner().getBuildings().add(Building.createBuilding(buildingType, this.getOwner()));
        }
        this.isBuilding.set(false);

    }

    /**
     * Determines if a peasant is free or not.
     * This means that the peasant is neither harvesting, nor building.
     *
     * @return Whether he is free
     */
    public boolean isFree(){
        return !isHarvesting.get() && !isBuilding.get();
    }


}
