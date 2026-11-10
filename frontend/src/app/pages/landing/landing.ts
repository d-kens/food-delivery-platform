import { NgOptimizedImage } from '@angular/common';
import { Component } from '@angular/core';
import { RouterLink } from '@angular/router';
import { Icon } from '../../shared/icon/icon';

interface Category {
  name: string;
  image: string;
}

interface Restaurant {
  name: string;
  cuisine: string;
  image: string;
  rating: number;
  reviews: string;
  deliveryTime: string;
  deliveryFee: string;
  promo?: string;
}

interface Step {
  title: string;
  description: string;
}

// Static placeholder content until restaurant-service exposes real data.
// Images are Unsplash photo ids, resolved by the IMAGE_LOADER in app.config.ts.
@Component({
  selector: 'app-landing',
  imports: [RouterLink, NgOptimizedImage, Icon],
  templateUrl: './landing.html',
})
export class Landing {
  protected readonly orderSteps = ['Ordered', 'Preparing', 'On the way', 'Delivered'];
  protected readonly activeOrderStep = 2;

  protected readonly neighbourhoods = ['Westlands', 'Kilimani', 'Karen', 'Lavington', 'CBD'];

  protected readonly categories: Category[] = [
    { name: 'Pizza', image: 'photo-1513104890138-7c749659a591' },
    { name: 'Burgers', image: 'photo-1568901346375-23c9450c58cd' },
    { name: 'Nyama Choma', image: 'photo-1555939594-58d7cb561ad1' },
    { name: 'Sushi', image: 'photo-1617196034796-73dfa7b1fd56' },
    { name: 'Chicken', image: 'photo-1626082927389-6cd097cdc6ec' },
    { name: 'Healthy', image: 'photo-1512621776951-a57141f2eefd' },
    { name: 'Curry', image: 'photo-1585937421612-70a008356fbe' },
    { name: 'Coffee', image: 'photo-1495474472287-4d71bcdd2085' },
  ];

  protected readonly restaurants: Restaurant[] = [
    {
      name: 'Urban Grill House',
      cuisine: 'Burgers',
      image: 'photo-1551782450-a2132b4ba21d',
      rating: 4.8,
      reviews: '2.1k',
      deliveryTime: '20–30 min',
      deliveryFee: 'Free delivery',
      promo: '20% off your first order',
    },
    {
      name: "Nonna's Pizzeria",
      cuisine: 'Italian',
      image: 'photo-1565299624946-b28f40a0ae38',
      rating: 4.7,
      reviews: '1.8k',
      deliveryTime: '25–35 min',
      deliveryFee: 'KSh 99',
    },
    {
      name: 'Choma Corner',
      cuisine: 'Kenyan · Grill',
      image: 'photo-1544025162-d76694265947',
      rating: 4.9,
      reviews: '3.4k',
      deliveryTime: '30–40 min',
      deliveryFee: 'KSh 149',
    },
    {
      name: 'Sakura Sushi Bar',
      cuisine: 'Japanese',
      image: 'photo-1579871494447-9811cf80d66c',
      rating: 4.6,
      reviews: '940',
      deliveryTime: '30–45 min',
      deliveryFee: 'KSh 199',
    },
    {
      name: 'Green Bowl Co.',
      cuisine: 'Healthy',
      image: 'photo-1546069901-ba9599a7e63c',
      rating: 4.7,
      reviews: '1.2k',
      deliveryTime: '15–25 min',
      deliveryFee: 'Free delivery',
    },
    {
      name: 'Spice Route',
      cuisine: 'Indian',
      image: 'photo-1601050690597-df0568f70950',
      rating: 4.8,
      reviews: '2.6k',
      deliveryTime: '25–35 min',
      deliveryFee: 'KSh 99',
      promo: 'Buy one, get one free',
    },
    {
      name: 'The Sweet Spot',
      cuisine: 'Desserts',
      image: 'photo-1565958011703-44f9829ba187',
      rating: 4.9,
      reviews: '1.5k',
      deliveryTime: '15–25 min',
      deliveryFee: 'Free delivery',
    },
    {
      name: 'Bean & Brew',
      cuisine: 'Coffee · Breakfast',
      image: 'photo-1509042239860-f550ce710b93',
      rating: 4.7,
      reviews: '860',
      deliveryTime: '10–20 min',
      deliveryFee: 'KSh 79',
    },
  ];

  protected readonly steps: Step[] = [
    {
      title: 'Tell us where you are',
      description: 'Enter your address and see every restaurant that delivers to you, with honest delivery times.',
    },
    {
      title: 'Pick what you are craving',
      description: 'Browse full menus, customise your order and pay by M-Pesa or card in a few taps.',
    },
    {
      title: 'Follow it to your door',
      description: 'Track your rider live on the map. We will let you know the moment your food arrives.',
    },
  ];

  protected onSearch(event: Event, address: string): void {
    event.preventDefault();
    // TODO: navigate to restaurant search once it exists.
    console.debug('Search restaurants near', address);
  }
}
