import { Component, OnInit } from '@angular/core';
import { Router } from '@angular/router';
import { MatLegacyTabChangeEvent } from '@angular/material/legacy-tabs';
import packageJson from '../../../package.json';

@Component({
  selector: 'app-home',
  templateUrl: './home.component.html',
  styleUrls: ['./home.component.scss'],
})
export class HomeComponent implements OnInit {
  public version: string = packageJson.version;

  constructor(private router: Router) {}

  update(index): void {}

  tabChanged(tabChangeEvent: MatLegacyTabChangeEvent): void {
    console.log('tabChangeEvent => ', tabChangeEvent);
    console.log('index => ', tabChangeEvent.index);
    this.update(tabChangeEvent.index);
  }

  async ngOnInit() {
    this.update(0);
  }
}
