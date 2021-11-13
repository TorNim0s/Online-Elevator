package ex0.algo;

import ex0.Building;
import ex0.CallForElevator;
import ex0.Elevator;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedList;

public class ElevAlgo implements ElevatorAlgo {
    public static final int UP = 1, DOWN = -1;
    private int _direction;
    private Building _building;

    ArrayList<Integer> _expressElevators;
    int _expressCounter;
    LinkedList<Integer>[] elevatorLink;

    public ElevAlgo(Building b) {
        _building = b;
        _direction = UP;
        elevatorLink = new LinkedList[_building.numberOfElevetors()];
        for (int i = 0; i < _building.numberOfElevetors(); i++) {
            elevatorLink[i] = new LinkedList<>();
        }
        _expressElevators = new ArrayList<>(); //array list for express elevators
        double speedAverage = speedAverage();
        for (int i = 0; i < _building.numberOfElevetors(); i++) {
            if(_building.getElevetor(i).getSpeed() >= speedAverage) {
                _expressElevators.add(i);
            }
        }
        _expressCounter = _expressElevators.size();
    }

    @Override
    public Building getBuilding() {
        return _building;
    }

    @Override
    public String algoName() {
        return "Ex0_OOP_Eldad_Ilan_Elevator_Algo";
    }

    //deciding which elevator is optimized for certain call
    @Override
    public int allocateAnElevator(CallForElevator c) {
        int Number_Of_Elevators = _building.numberOfElevetors();
        int Floor_Called = c.getSrc();
        int Call_State = c.getType();
        if (Floor_Called > _building.maxFloor() * 3 || Floor_Called < _building.minFloor() / 2) { //using the express elevators
            int express = checkExpress(c);
            if (express != -1) {
                return express;
            }
        }

        int step1 = first_step(c, Number_Of_Elevators, Floor_Called, Call_State);
        if (step1 != -1) {
            return step1;
        }

        int step2 = second_step(c, Number_Of_Elevators, Floor_Called);
        if (step2 != -1) {
            return step2;
        }

        int step3 = third_step(c, Number_Of_Elevators, Floor_Called);
        return step3;
    }

    //making sure there is an express elevator available for the call
    public int checkExpress(CallForElevator c) {
        int elev = -1;
        for (int i = 0; i < _expressCounter; i++) {
            int curr = _expressElevators.get(i);
            if (_building.getElevetor(curr).getState() == Elevator.LEVEL) {
                elev = _expressElevators.get(i);
                elevatorLink[elev].addLast(c.getSrc());
                elevatorLink[elev].addFirst(c.getDest());
                break;
            }
        }
        return elev;
    }

    //based the algorithm in part 0, checking if there is an elevator that his destination
    //is the source of the call
    public int first_step(CallForElevator c, int numElev, int floor, int state) {
        for (int i = 0; i < numElev; i++) {
            if (!elevatorLink[i].isEmpty()) {
                if (elevatorLink[i].getFirst() != c.getDest()) continue;
            }
            Elevator Current_Elevator = _building.getElevetor(i);
            if (Current_Elevator.getState() == state) {
                if (state == UP) { //checking for the direction of the call
                    if (Current_Elevator.getPos() <= floor) { //checking if the elevator didn't pass the floor of the call
                        elevatorLink[i].addLast(floor);
                        elevatorLink[i].addFirst(c.getDest());
                        fixLinkedList(i);
                        return i;
                    }
                } else { //down
                    if (Current_Elevator.getPos() >= floor) {
                        elevatorLink[i].addLast(floor);
                        elevatorLink[i].addFirst(c.getDest());
                        fixLinkedList(i);
                        return i;
                    }
                }
            }
        }
        return -1;
    }

    //sorting the linked list to make sure it sorted towards the direction of the elevator
    public void fixLinkedList(int elev) {
        int head = elevatorLink[elev].getFirst();
        elevatorLink[elev].removeFirst();
        if (_building.getElevetor(elev).getPos() > head) {
            elevatorLink[elev].sort(Comparator.reverseOrder());
        } else {
            Collections.sort(elevatorLink[elev]);
        }
        elevatorLink[elev].addFirst(head);
    }

    //based on the algorithm in part 0, looking at all the idle elevators and deciding which elevator
    //is optimized to the call based on distance and time
    public int second_step(CallForElevator c, int numElev, int floor) {
        ArrayList<Elevator> ElevatorList = new ArrayList<>();
        for (int i = 0; i < numElev; i++) { //counting all the idle elevators
            Elevator Current_Elevator = _building.getElevetor(i);
            if (Current_Elevator.getState() == Elevator.LEVEL) {
                ElevatorList.add(Current_Elevator);
            }
        }
        int EleInd = findDiff(floor, ElevatorList); //checking for the best distance/time ratio
        if (EleInd != -1) {
            elevatorLink[EleInd].addLast(floor);
            elevatorLink[EleInd].addFirst(c.getDest());
            fixLinkedList(EleInd);
        }
        return EleInd;
    }

    //function that calculates the distance using time
    private double dist(int src, int elev) {
        double ans;
        Elevator thisElev = this._building.getElevetor(elev);
        int pos = thisElev.getPos();
        double speed = thisElev.getSpeed();
        double floorTime = thisElev.getStopTime() + thisElev.getStartTime() + thisElev.getTimeForOpen() + thisElev.getTimeForClose();
        ans = Math.abs(src - pos) * (1 / speed) + floorTime;
        return ans;
    }

    //based on the algorithm in part 0, looking at all the moving elevators and checking the
    //best elevator which can answer the call after completing the previous call
    public int third_step(CallForElevator c, int numElev, int floor) {
        ArrayList<Elevator> ElevatorList = new ArrayList<>();
        for (int i = 0; i < numElev; i++) {
            Elevator Current_Elevator = _building.getElevetor(i);
            if (Current_Elevator.getState() != Elevator.LEVEL) {
                ElevatorList.add(Current_Elevator);
            }
        }
        int EleInd = findDiff(floor, ElevatorList);
        elevatorLink[EleInd].addLast(floor);
        elevatorLink[EleInd].addFirst(c.getDest());
        return EleInd;
    }

    //finding the optimized elevator by comparing the distances from the call floor
    private int findDiff(int floor, ArrayList<Elevator> elevatorList) {
        int EleInd = -1;
        double max = Integer.MAX_VALUE;
        for (Elevator elevator : elevatorList) {
            double diff = dist(floor, elevator.getID());
            if (diff < max) {
                max = diff;
                EleInd = elevator.getID();
            }
        }
        return EleInd;
    }

    //finding the average speed of all the elevators in the building
    private double speedAverage(){
        double average = 0;
        for (int i = 0; i < _building.numberOfElevetors(); i++) {
            average+= _building.getElevetor(i).getSpeed();
        }
        return average/_building.numberOfElevetors();
    }

    //function that sends each elevator to desired location using the data in the linked list
    //that was provided in the allocateAnElevator function
    @Override
    public void cmdElevator(int EleInd) {
        Elevator elevator = _building.getElevetor(EleInd);
        if (elevator.getState() == Elevator.LEVEL) {
            if (!elevatorLink[EleInd].isEmpty()) {
                elevator.goTo(elevatorLink[EleInd].getLast());
                elevatorLink[EleInd].removeLast();
            }
        }
    }
}
